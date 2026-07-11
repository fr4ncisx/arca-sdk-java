package io.github.fr4ncisx.arca.wsfev1.model.batch;

/**
 * Strategy to process a batch of electronic invoices.
 *
 * @author fr4ncisx
 * @since 0.4.0-M1
 */
public enum BatchStrategy {

    /**
     * Processes requests one after another synchronously.
     */
    SEQUENTIAL,

    /**
     * Distributes execution concurrently using a pool of threads.
     * The number of simultaneous network requests is capped.
     */
    PARALLEL_LIMITED,

    /**
     * Cancels pending requests immediately if a network timeout or connection error occurs,
     * returning the successfully processed requests up to that point.
     */
    FAIL_FAST
}
