package io.github.fr4ncisx.arca.wsaa.internal.cache;

import io.github.fr4ncisx.arca.core.clock.ArcaClock;
import io.github.fr4ncisx.arca.core.exception.ArcaAuthException;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.wsaa.model.ArcaAccessTicket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

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
 * {@link #clear()} only deletes files owned by this cache. Technical I/O issues
 * are caught and translated to {@link ArcaAuthException}.
 * <p>
 * The directory must be writable even for reads. Cooperating instances must use
 * the same path and retain the stable lock file. Writes require atomic replacement
 * support on the deployment filesystem. Locks protect individual cache operations,
 * not the entire authentication sequence across clients or processes.
 *
 * @author fr4ncisx
 * @since 0.3.0-M1
 */
public final class PersistentTicketCache implements TicketCache {

    private static final String CACHE_FILE_SUFFIX = ".cache";
    private static final String LOCK_FILE_NAME = ".ticket-cache.lock";
    private static final String TEMP_FILE_PREFIX = ".arca-sdk-ticket-";
    private static final String TEMP_FILE_SUFFIX = ".tmp";
    private static final Set<PosixFilePermission> RESTRICTED_FILE_PERMISSIONS =
            PosixFilePermissions.fromString("rw-------");
    private static final Set<PosixFilePermission> RESTRICTED_DIRECTORY_PERMISSIONS =
            PosixFilePermissions.fromString("rwx------");
    private static final ReentrantLock[] DIRECTORY_LOCKS = createDirectoryLocks();

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
        this.directory = directory.toAbsolutePath().normalize();
        this.clock = clock;
    }

    /**
     * Resolves the ticket for the key from disk, deleting it if expired.
     *
     * @param key cache key associated with the ticket (e.g. the service name)
     * @return the valid cached ticket, or empty if missing or expired
     * @throws ArcaValidationException if key is null or blank
     * @throws ArcaAuthException       if the file is corrupt, inaccessible, or the filesystem lacks required capabilities
     */
    @Override
    public Optional<ArcaAccessTicket> get(String key) {
        validateKey(key);
        if (!Files.isDirectory(directory)) {
            return Optional.empty();
        }

        Path filePath = resolvePath(key);
        try {
            return withDirectoryLock(() -> readTicket(key, filePath));
        } catch (NoSuchFileException exception) {
            return Optional.empty();
        } catch (IOException | IllegalArgumentException | UnsupportedOperationException exception) {
            throw new ArcaAuthException("Failed to read persistent ticket cache file for key: " + key, exception);
        }
    }

    /**
     * Persists the ticket to disk for the specified key.
     *
     * @param key    cache key associated with the ticket
     * @param ticket the ticket to store
     * @throws ArcaValidationException if key or ticket is null
     * @throws ArcaAuthException       if writing fails or the filesystem lacks required capabilities
     */
    @Override
    public void put(String key, ArcaAccessTicket ticket) {
        validateKey(key);
        if (ticket == null) {
            throw new ArcaValidationException("ticket must not be null");
        }

        Path filePath = resolvePath(key);
        try {
            withDirectoryLock(() -> {
                writeTicket(filePath, ticket);
                return null;
            });
        } catch (IOException | UnsupportedOperationException exception) {
            throw new ArcaAuthException("Failed to write persistent ticket cache file for key: " + key, exception);
        }
    }

    /**
     * Deletes the ticket file associated with the key from disk.
     *
     * @param key cache key to evict
     * @throws ArcaValidationException if key is null or blank
     * @throws ArcaAuthException       if deletion fails or the filesystem lacks required capabilities
     */
    @Override
    public void evict(String key) {
        validateKey(key);
        if (!Files.isDirectory(directory)) {
            return;
        }

        Path filePath = resolvePath(key);
        try {
            withDirectoryLock(() -> Files.deleteIfExists(filePath));
        } catch (IOException | UnsupportedOperationException exception) {
            throw new ArcaAuthException("Failed to delete persistent ticket cache file for key: " + key, exception);
        }
    }

    /**
     * Clears every ticket file and temporary write owned by this cache.
     *
     * @throws ArcaAuthException if listing or deletion fails, or the filesystem lacks required capabilities
     */
    @Override
    public void clear() {
        if (!Files.isDirectory(directory)) {
            return;
        }

        try {
            withDirectoryLock(() -> {
                try (var paths = Files.list(directory)) {
                    for (Path path : paths.toList()) {
                        if (isOwnedCacheFile(path)) {
                            Files.deleteIfExists(path);
                        }
                    }
                }
                return null;
            });
        } catch (IOException | UnsupportedOperationException exception) {
            throw new ArcaAuthException("Failed to clear persistent ticket cache directory", exception);
        }
    }

    private Optional<ArcaAccessTicket> readTicket(String key, Path filePath) throws IOException {
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(filePath)) {
            properties.load(input);
        }

        String token = properties.getProperty("token");
        String sign = properties.getProperty("sign");
        String generationTimeText = properties.getProperty("generationTime");
        String expirationTimeText = properties.getProperty("expirationTime");
        if (token == null || sign == null || generationTimeText == null || expirationTimeText == null) {
            throw new ArcaAuthException("Corrupt ticket cache structure for key: " + key);
        }

        ArcaAccessTicket ticket;
        try {
            ticket = new ArcaAccessTicket(
                    token,
                    sign,
                    Instant.parse(generationTimeText),
                    Instant.parse(expirationTimeText));
        } catch (DateTimeParseException | ArcaValidationException exception) {
            throw new ArcaAuthException("Failed to parse persistent ticket timestamps for key: " + key, exception);
        }

        if (ticket.isExpired(clock)) {
            Files.deleteIfExists(filePath);
            return Optional.empty();
        }
        return Optional.of(ticket);
    }

    private void writeTicket(Path filePath, ArcaAccessTicket ticket) throws IOException {
        Properties properties = new Properties();
        properties.setProperty("token", ticket.token());
        properties.setProperty("sign", ticket.sign());
        properties.setProperty("generationTime", ticket.generationTime().toString());
        properties.setProperty("expirationTime", ticket.expirationTime().toString());

        Path temporaryFile = createRestrictedTemporaryFile();
        try {
            try (OutputStream output = Files.newOutputStream(
                    temporaryFile,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING)) {
                properties.store(output, "ARCA SDK WSAA Access Ticket Cache File");
            }
            Files.move(
                    temporaryFile,
                    filePath,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    private <T> T withDirectoryLock(IoOperation<T> operation) throws IOException {
        ReentrantLock processLock = directoryLock();
        processLock.lock();
        try {
            Files.createDirectories(directory);
            restrictDirectoryPermissions();
            Path lockPath = directory.resolve(LOCK_FILE_NAME);
            prepareRestrictedFile(lockPath);
            try (FileChannel channel = FileChannel.open(
                    lockPath,
                    StandardOpenOption.READ,
                    StandardOpenOption.WRITE);
                 FileLock ignored = channel.lock()) {
                return operation.run();
            }
        } finally {
            processLock.unlock();
        }
    }

    private ReentrantLock directoryLock() {
        int stripe = Math.floorMod(directory.hashCode(), DIRECTORY_LOCKS.length);
        return DIRECTORY_LOCKS[stripe];
    }

    private Path createRestrictedTemporaryFile() throws IOException {
        if (supportsPosix()) {
            return Files.createTempFile(
                    directory,
                    TEMP_FILE_PREFIX,
                    TEMP_FILE_SUFFIX,
                    PosixFilePermissions.asFileAttribute(RESTRICTED_FILE_PERMISSIONS));
        }
        return Files.createTempFile(directory, TEMP_FILE_PREFIX, TEMP_FILE_SUFFIX);
    }

    private static ReentrantLock[] createDirectoryLocks() {
        ReentrantLock[] locks = new ReentrantLock[64];
        for (int index = 0; index < locks.length; index++) {
            locks[index] = new ReentrantLock();
        }
        return locks;
    }

    private static boolean isOwnedCacheFile(Path path) {
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            return false;
        }
        Path fileName = path.getFileName();
        if (fileName == null) {
            return false;
        }
        String name = fileName.toString();
        return name.endsWith(CACHE_FILE_SUFFIX)
                || (name.startsWith(TEMP_FILE_PREFIX) && name.endsWith(TEMP_FILE_SUFFIX));
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
        try {
            if (supportsPosix()) {
                Files.createFile(filePath, PosixFilePermissions.asFileAttribute(RESTRICTED_FILE_PERMISSIONS));
            } else {
                Files.createFile(filePath);
            }
        } catch (FileAlreadyExistsException exception) {
            if (supportsPosix()) {
                Files.setPosixFilePermissions(filePath, RESTRICTED_FILE_PERMISSIONS);
            }
        }
    }

    @FunctionalInterface
    private interface IoOperation<T> {

        T run() throws IOException;
    }
}
