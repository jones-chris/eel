package io.eel.service;//package io.eel.service;
//
//import com.google.gson.Gson;
//import io.eel.exception.ThreadSleepInterruptedException;
//import io.eel.exception.WorkbookPoolException;
//import io.eel.model.proxy.WorkbookProxy;
//import lombok.extern.log4j.Log4j2;
//import org.apache.poi.ss.usermodel.Workbook;
//import org.apache.poi.ss.usermodel.WorkbookFactory;
//
//import java.io.File;
//import java.io.IOException;
//import java.io.InputStream;
//import java.util.Arrays;
//import java.util.List;
//import java.util.Objects;
//import java.util.concurrent.atomic.AtomicReference;
//
//@Log4j2
//public class WorkbookPoolImpl implements WorkbookPool {
//
//    private static final String EEL_XLSX_RESOURCE_FILE_PATH = "/eel.xlsx";
//
//    private static final Gson gson = new Gson();
//
//    private static final WorkbookProxy ALPHA_WORKBOOK;
//
//    private final AtomicReference<List<WorkbookProxy>> availableWorkbooks;
//
//    private final AtomicReference<List<WorkbookProxy>> unavailableWorkbooks;
//
//    private final int maxAttempts;
//
//    private final int baseExponentialBackoffInSeconds;
//
//    static {
//        // Instantiate the mother workbook only once because it's being read from the file.
//        try {
//            InputStream inputStream = WorkbookPoolImpl.class.getResourceAsStream(EEL_XLSX_RESOURCE_FILE_PATH);
//            Objects.requireNonNull(inputStream);
//
//            Workbook workbook = WorkbookFactory.create(inputStream);
//
//            ALPHA_WORKBOOK = new WorkbookProxy(workbook);
//        } catch (IOException e) {
//            log.error(e);
//            throw new RuntimeException(e); // todo:  don't throw a RuntimeException
//        }
//    }
//
//    public WorkbookPoolImpl(Integer poolSize, Integer maxAttempts, Integer baseExponentialBackoffInSeconds) {
//        // Create fixed arrays to save memory, but wrap them in a list so it's easier to access them.
//        this.availableWorkbooks = new AtomicReference<>(Arrays.asList(new WorkbookProxy[(poolSize == null) ? Defaults.POOL_SIZE : poolSize]));
//        this.unavailableWorkbooks = new AtomicReference<>(Arrays.asList(new WorkbookProxy[(poolSize == null) ? Defaults.POOL_SIZE : poolSize]));
//
//        this.maxAttempts = (maxAttempts == null)
//                ? Defaults.MAX_ATTEMPTS
//                : maxAttempts;
//        this.baseExponentialBackoffInSeconds = (baseExponentialBackoffInSeconds == null)
//                ? Defaults.BASE_EXPONENTIAL_BACKOFF_IN_SECONDS
//                : baseExponentialBackoffInSeconds;
//        poolSize = (poolSize == null)
//                ? Defaults.POOL_SIZE
//                : poolSize;
//
//        // Create the workbook pool from the mother workbook.
//        for (int i = 0; i < poolSize; i++) {
//            this.createWorkerWorkbook();
//        }
//    }
//
//    /**
//     * Returns a workbook from the pool using exponential backoff if one is not available.  If a workbook is not available
//     * within the exponential backoff time period, then {@link WorkbookPoolException} is thrown.
//     *
//     * @return {@link WorkbookProxy}
//     */
//    @Override
//    public WorkbookProxy getWorkbookProxy() {
//        int totalAttempts = 0;
//        WorkbookProxy workbookProxy = null;
//
//        do {
//            // Increment the number of total attempts.
//            totalAttempts++;
//
//            // Attempt to get an available workbook from the pool...
//            if (! this.availableWorkbooks.get().isEmpty()) {
//                workbookProxy = this.availableWorkbooks.get().removeFirst();
//                this.unavailableWorkbooks.get().add(workbookProxy);
//            } else {
//                // ...if one is not available, then sleep the thread (with exponential backoff) and try again up to the max attempts.
//                final long sleepInMillis = (long) this.baseExponentialBackoffInSeconds * totalAttempts * 1000;
//                try {
//                    Thread.sleep(sleepInMillis);
//                } catch (InterruptedException e) {
//                    throw new ThreadSleepInterruptedException();
//                }
//            }
//        } while (workbookProxy == null && totalAttempts < this.maxAttempts);
//
//        // If we have finished the exponential backoff loop and the workbook is null, then no workbook is available in the pool
//        // and an unrecoverable exception should be thrown.
//        if (workbookProxy == null) {
//            throw new WorkbookPoolException();
//        }
//
//        // If it's not null, then return it.
//        return workbookProxy;
//    }
//
//    /**
//     * Releases the workbook to the pool of available workbooks.
//     * @param workbookProxy {@link WorkbookProxy}
//     */
//    @Override
//    public void releaseWorkbookProxy(WorkbookProxy workbookProxy) {
//        this.unavailableWorkbooks.get().remove(workbookProxy);
//        this.createWorkerWorkbook();
//    }
//
//    private void createWorkerWorkbook() {
//        InputStream inputStream = WorkbookPoolImpl.class.getResourceAsStream(EEL_XLSX_RESOURCE_FILE_PATH);
//        Objects.requireNonNull(inputStream);
//
//        try {
//            Workbook workbook = WorkbookFactory.create(inputStream);
//            WorkbookProxy workbookProxy = new WorkbookProxy(workbook);
//
//            this.availableWorkbooks.get().add(workbookProxy);
//        } catch (IOException e) {
//            log.error(e);
//            throw new WorkbookPoolException();
//        }
//
//
//    }
//
//    /**
//     * Private inner class that contains defaults for {@link WorkbookPoolImpl} configuration fields.
//     */
//    private static class Defaults {
//
//        public static final int POOL_SIZE = 5;
//
//        public static final int MAX_ATTEMPTS = 3;
//
//        public static final int BASE_EXPONENTIAL_BACKOFF_IN_SECONDS = 5;
//
//    }
//
//}
