package io.github.fr4ncisx.arca.wsaa.internal.cache;

import io.github.fr4ncisx.arca.core.clock.ArcaClock;
import io.github.fr4ncisx.arca.core.exception.ArcaAuthException;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.wsaa.model.ArcaAccessTicket;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

/**
 * File-system based implementation of the WSAA ticket cache.
 * <p>
 * This cache stores access tickets on disk as standard properties files
 * within a dedicated cache directory, enabling session persistence across
 * application restarts.
 * <p>
 * Because tickets hold live credentials, persisted files are restricted to
 * their owner on POSIX-compliant filesystems ({@code rw-------} for files and
 * {@code rwx------} for the cache directory). On filesystems without POSIX
 * support, platform default permissions apply and the directory should be
 * pre-configured by the operator with equivalent restrictions.
 * <p>
 * Expired tickets are cleaned up lazily from disk during read operations.
 * {@link #clear()} only deletes files owned by this cache (the
 * {@code *.cache} entries inside the configured directory). Technical I/O
 * issues are caught and translated to {@link ArcaAuthException}.
 *
 * @author fr4ncisx
 * @since 0.3.0-M1
 */
public final class PersistentTicketCache implements TicketCache {

    private static final String CACHE_FILE_SUFFIX = ".cache";
    private static final Set<PosixFilePermission> RESTRICTED_FILE_PERMISSIONS =
            PosixFilePermissions.fromString("rw-------");
    private static final Set<PosixFilePermission> RESTRICTED_DIRECTORY_PERMISSIONS =
            PosixFilePermissions.fromString("rwx------");

    private final Path directory;
    private final ArcaClock clock;

    /**
     * Creates a new persistent ticket cache in the specified directory.
     *
     * @param directory the directory where ticket cache files will be stored
     * @param clock     the clock used to evaluate ticket expiration
     * @throws ArcaValidationException if directory or clock is null
     */
    public PersistentTicketCache(Path directory, ArcaClock clock) {
        if (directory == null) {
            throw new ArcaValidationException("directory must not be null");
        }
        if (clock == null) {
            throw new ArcaValidationException("clock must not be null");
        }
        this.directory = directory;
        this.clock = clock;
    }

    /**
     * Resolves the ticket for the key from disk, deleting it if expired.
     *
     * @param key cache key associated with the ticket (e.g. the service name)
     * @return the valid cached ticket, or empty if missing or expired
     * @throws ArcaValidationException if key is null or blank
     * @throws ArcaAuthException       if the file is corrupt or an I/O error occurs
     */
    @Override
    public Optional<ArcaAccessTicket> get(String key) {
        validateKey(key);
        Path filePath = resolvePath(key);
        Properties props = new Properties();
        try (FileChannel channel = FileChannel.open(filePath, StandardOpenOption.READ);
             FileLock lock = channel.lock(0, Long.MAX_VALUE, true)) {
            props.load(Channels.newInputStream(channel));
        } catch (java.nio.file.NoSuchFileException e) {
            return Optional.empty();
        } catch (IOException | IllegalArgumentException e) {
            throw new ArcaAuthException("Failed to read persistent ticket cache file for key: " + key, e);
        }

        String token = props.getProperty("token");
        String sign = props.getProperty("sign");
        String genStr = props.getProperty("generationTime");
        String expStr = props.getProperty("expirationTime");

        if (token == null || sign == null || genStr == null || expStr == null) {
            throw new ArcaAuthException("Corrupt ticket cache structure for key: " + key);
        }

        ArcaAccessTicket ticket;
        try {
            Instant generationTime = Instant.parse(genStr);
            Instant expirationTime = Instant.parse(expStr);
            ticket = new ArcaAccessTicket(token, sign, generationTime, expirationTime);
        } catch (Exception e) {
            throw new ArcaAuthException("Failed to parse persistent ticket timestamps for key: " + key, e);
        }

        if (ticket.isExpired(clock)) {
            evict(key);
            return Optional.empty();
        }

        return Optional.of(ticket);
    }

    /**
     * Persists the ticket to disk for the specified key.
     *
     * @param key    cache key associated with the ticket
     * @param ticket the ticket to store
     * @throws ArcaValidationException if key or ticket is null
     * @throws ArcaAuthException       if an I/O error occurs writing to disk
     */
    @Override
    public void put(String key, ArcaAccessTicket ticket) {
        validateKey(key);
        if (ticket == null) {
            throw new ArcaValidationException("ticket must not be null");
        }

        Path filePath = resolvePath(key);
        try {
            Files.createDirectories(directory);
            restrictDirectoryPermissions();
            Properties props = new Properties();
            props.setProperty("token", ticket.token());
            props.setProperty("sign", ticket.sign());
            props.setProperty("generationTime", ticket.generationTime().toString());
            props.setProperty("expirationTime", ticket.expirationTime().toString());

            prepareRestrictedFile(filePath);
            try (FileChannel channel = FileChannel.open(filePath, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
                 FileLock lock = channel.lock()) {
                props.store(Channels.newOutputStream(channel), "ARCA SDK WSAA Access Ticket Cache File");
            }
        } catch (IOException e) {
            throw new ArcaAuthException("Failed to write persistent ticket cache file for key: " + key, e);
        }
    }

    /**
     * Deletes the ticket file associated with the key from disk.
     *
     * @param key cache key to evict
     * @throws ArcaValidationException if key is null or blank
     * @throws ArcaAuthException       if an I/O error occurs deleting the file
     */
    @Override
    public void evict(String key) {
        validateKey(key);
        Path filePath = resolvePath(key);
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new ArcaAuthException("Failed to delete persistent ticket cache file for key: " + key, e);
        }
    }

    /**
     * Clears every cached ticket file owned by this cache in the directory.
     * <p>
     * Only regular files ending with {@code *.cache} are deleted; unrelated
     * files sharing the configured directory are preserved.
     *
     * @throws ArcaAuthException if an I/O error occurs listing or deleting files
     */
    @Override
    public void clear() {
        try {
            if (Files.exists(directory)) {
                try (var stream = Files.list(directory)) {
                    for (Path path : stream.toList()) {
                        if (isOwnedCacheFile(path)) {
                            Files.delete(path);
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new ArcaAuthException("Failed to clear persistent ticket cache directory", e);
        }
    }

    private static boolean isOwnedCacheFile(Path path) throws IOException {
        if (!Files.isRegularFile(path)) {
            return false;
        }
        var fileName = path.getFileName();
        return fileName != null && fileName.toString().endsWith(CACHE_FILE_SUFFIX);
    }

    private Path resolvePath(String key) {
        String sanitizedKey = key.replaceAll("[^a-zA-Z0-9_-]", "_");
        return directory.resolve(sanitizedKey + CACHE_FILE_SUFFIX);
    }

    private static void validateKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            throw new ArcaValidationException("key must not be null or blank");
        }
    }

    private static boolean supportsPosix() {
        return FileSystems.getDefault().supportedFileAttributeViews().contains("posix");
    }

    private void restrictDirectoryPermissions() throws IOException {
        if (supportsPosix()) {
            Files.setPosixFilePermissions(directory, RESTRICTED_DIRECTORY_PERMISSIONS);
        }
    }

    private static void prepareRestrictedFile(Path filePath) throws IOException {
        if (!supportsPosix()) {
            return;
        }
        try {
            Files.createFile(filePath, PosixFilePermissions.asFileAttribute(RESTRICTED_FILE_PERMISSIONS));
        } catch (FileAlreadyExistsException e) {
            Files.setPosixFilePermissions(filePath, RESTRICTED_FILE_PERMISSIONS);
        }
    }
}
