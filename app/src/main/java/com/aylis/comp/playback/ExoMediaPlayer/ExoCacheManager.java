package com.aylis.comp.playback.ExoMediaPlayer;

import android.content.Context;
import android.net.Uri;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.database.StandaloneDatabaseProvider;
import androidx.media3.datasource.DataSpec;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.datasource.cache.CacheDataSource;
import androidx.media3.datasource.cache.CacheWriter;
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor;
import androidx.media3.datasource.cache.SimpleCache;
import java.io.File;

@UnstableApi
public class ExoCacheManager {

    private static SimpleCache simpleCache;
    private static final long MAX_CACHE_SIZE = 150 * 1024 * 1024; // 150 MB
    private static final long PRE_CACHE_BYTES = 2 * 1024 * 1024; // 2 MB
    
    private static CacheDataSource.Factory cacheDataSourceFactory;

    public static synchronized void init(Context context) {
        if (simpleCache == null) {
            File cacheDir = new File(context.getCacheDir(), "exo_media_cache");
            LeastRecentlyUsedCacheEvictor evictor = new LeastRecentlyUsedCacheEvictor(MAX_CACHE_SIZE);
            StandaloneDatabaseProvider databaseProvider = new StandaloneDatabaseProvider(context);
            simpleCache = new SimpleCache(cacheDir, evictor, databaseProvider);
            
            String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36";
            DefaultHttpDataSource.Factory httpDataSourceFactory = new DefaultHttpDataSource.Factory().setUserAgent(userAgent);
            
            cacheDataSourceFactory = new CacheDataSource.Factory()
                .setCache(simpleCache)
                .setUpstreamDataSourceFactory(httpDataSourceFactory)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR);
        }
    }

    public static CacheDataSource.Factory getCacheDataSourceFactory(Context context) {
        if (simpleCache == null) {
            init(context);
        }
        return cacheDataSourceFactory;
    }

    public static SimpleCache getSimpleCache(Context context) {
        if (simpleCache == null) {
            init(context);
        }
        return simpleCache;
    }

    public static void preCache(Context context, Uri uri) {
        if (uri == null) return;
        
        if (simpleCache == null) {
            init(context);
        }

        new Thread(() -> {
            try {
                DataSpec dataSpec = new DataSpec.Builder()
                        .setUri(uri)
                        .setLength(PRE_CACHE_BYTES)
                        .build();
                        
                CacheWriter cacheWriter = new CacheWriter(
                        cacheDataSourceFactory.createDataSource(),
                        dataSpec,
                        null,
                        null);
                        
                cacheWriter.cache();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
