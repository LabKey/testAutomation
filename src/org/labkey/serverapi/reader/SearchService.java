package org.labkey.serverapi.reader;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Reader;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

public interface SearchService
{
    // create logger for package which can be set via logger-manage.view
    Logger _packageLogger = Logger.getLogger(SearchService.class.getPackage().getName());
    Logger _log = Logger.getLogger(SearchService.class);

    @Deprecated //Use getFileSizeLimit() method instead
            long FILE_SIZE_LIMIT = 100L*(1024*1024); // 100 MB  //Keeping in case this is used in non-managed code

    long DEFAULT_FILE_SIZE_LIMIT = 100L; // 100 MB

    /**
     * Returns the max file size indexed (in bytes)
     * @return
     */
    default long getFileSizeLimit()
    {
        return DEFAULT_FILE_SIZE_LIMIT * (1024*1024);
    }

    SearchCategory navigationCategory = new SearchCategory("navigation", "internal category", false);
    SearchCategory fileCategory = new SearchCategory("file", "Files and Attachments", false);

    // marker value for documents with indexing errors
    Date failDate = new Timestamp(DateUtil.parseISODateTime("1899-12-30"));


    enum PRIORITY
    {
        commit,

        idle,       // only used to detect when there is no other work to do
        crawl,      // lowest work priority
        background, // crawler item

        bulk,       // all wikis
        group,      // one container
        item,       // one page/attachment
        delete
    }


    enum PROPERTY
    {
        title("title"),
        keywordsLo("keywordsLo"),
        keywordsMed("keywordsMed"),
        keywordsHi("keywordsHi"),
        identifiersLo("identifiersLo"),
        identifiersMed("identifiersMed"),
        identifiersHi("identifiersHi"),
        categories("searchCategories"),
        summary("summary");

        private final String _propName;

        PROPERTY(String name)
        {
            _propName = name;
        }

        @Override
        public String toString()
        {
            return _propName;
        }
    }

    enum SEARCH_PHASE {createQuery, buildSecurityFilter, search, applySecurityFilter, processHits}



    interface IndexTask extends Future<IndexTask>
    {
        String getDescription();

        int getDocumentCountEstimate();

        int getIndexedCount();

        int getFailedCount();

        long getStartTime();

        long getCompleteTime();

        void log(String message);

        Reader getLog();

        void addToEstimate(int i);// indicates that caller is done adding Resources to this task

        /**
         * indicates that we're done adding the initial set of resources/runnables to this task
         * the task be considered done after calling setReady() and there is no more work to do.
         */
        void setReady();

        void addRunnable(@NotNull Runnable r, @NotNull SearchService.PRIORITY pri);

        void addResource(@NotNull String identifier, SearchService.PRIORITY pri);

    }





    class SearchCategory
    {
        private final String _name;
        private final String _description;
        private final boolean _showInDialog;

        public SearchCategory(@NotNull String name, @NotNull String description)
        {
            this(name,description,true);
        }

        public SearchCategory(@NotNull String name, @NotNull String description, boolean showInDialog)
        {
            _name = name;
            _description = description;
            _showInDialog = showInDialog;
        }

        public String getName()
        {
            return _name;
        }

        public String getDescription()
        {
            return _description;
        }

        @Override
        public String toString()
        {
            return _name;
        }
    }


    Map<String, String> getIndexFormatProperties();

    List<Pair<String, String>> getDirectoryTypes();

    String escapeTerm(String term);

    List<SearchCategory> getSearchCategories();

    //
    // index
    //

    void purgeQueues();
    void start();
    void resetIndex();
    void startCrawler();
    void pauseCrawler();
    void updateIndex();
    @Nullable Throwable getConfigurationError();
    boolean isRunning();

    IndexTask defaultTask();
    IndexTask createTask(String description);


    void deleteResource(String identifier);

    // Delete all resources whose documentIds starts with the given prefix
    void deleteResourcesForPrefix(String prefix);

    List<IndexTask> getTasks();

    void addPathToCrawl(Path path, @Nullable Date nextCrawl);


    /** an indicator that there are a lot of things in the queue */
    boolean isBusy();
    void waitForIdle() throws InterruptedException;


    /** default implementation saving lastIndexed */
    void setLastIndexedForPath(Path path, long indexed, long modified);

    void deleteContainer(String id);

    void deleteIndex();          // delete the index directory and reset lastIndexed values. must be called before start() has been called.
    void clear();                // clear index and reset lastIndexed values. must be callable before (and after) start() has been called.

    @Deprecated // Not used... after testing deleteIndex(), should remove this and lucene-backward-codecs.jar
    void upgradeIndex();         // upgrade to latest format. this must be called before the SearchService is started.

    void clearLastIndexed();     // just reset lastIndexed values. must be callable before (and after) start() has been called.
    void maintenance();


}
