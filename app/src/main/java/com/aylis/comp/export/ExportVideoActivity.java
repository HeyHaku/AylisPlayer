package com.aylis.comp.export;

import android.app.Activity;
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.aylis.R;

public class ExportVideoActivity extends Activity {
    private android.view.View layoutDownloadProgress;
    private android.view.View layoutExportProgress;
    private TextView txtDownloadProgress;
    private ProgressBar downloadProgressBar;
    private ProgressBar exportProgressBar;
    private TextView txtExportProgress;
    private Button btnCancel;
    private VideoExportTask exportTask;
    private ValueAnimator progressAnimator;
    private int currentDisplayedProgress = 0;
    private int targetProgressForBar = 0;

    public static boolean isExporting = false;

    private static final int PICK_AUDIO_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_export_video);
        
        layoutDownloadProgress = findViewById(R.id.layoutDownloadProgress);
        layoutExportProgress = findViewById(R.id.layoutExportProgress);
        txtDownloadProgress = findViewById(R.id.txtDownloadProgress);
        downloadProgressBar = findViewById(R.id.downloadProgressBar);
        exportProgressBar = findViewById(R.id.exportProgressBar);
        txtExportProgress = findViewById(R.id.txtExportProgress);
        btnCancel = findViewById(R.id.btnCancelExport);
        
        btnCancel.setOnClickListener(v -> {
            if (exportTask != null) {
                exportTask.cancel();
            }
            isExporting = false;
            finish();
        });
        
        String audioUriStr = getIntent().getStringExtra("audio_uri");
        String themeJson = getIntent().getStringExtra("theme_json");
        String trackName = getIntent().getStringExtra("track_name");
        if (trackName == null) trackName = "";
        int width = getIntent().getIntExtra("width", 1080);
        int height = getIntent().getIntExtra("height", 1080);
        int fps = getIntent().getIntExtra("fps", 60);
        int bitrate = getIntent().getIntExtra("bitrate", 10000000);
        int startSec = getIntent().getIntExtra("startSec", 0);
        int endSec = getIntent().getIntExtra("endSec", 15);
        
        String customFilename = getIntent().getStringExtra("custom_filename");
        if (customFilename != null && !customFilename.isEmpty()) {
            trackName = customFilename;
        }
        String outputFolderUri = getIntent().getStringExtra("output_folder_uri");

        if (audioUriStr != null && !audioUriStr.isEmpty()) {
            startExport(android.net.Uri.parse(audioUriStr), themeJson, trackName, outputFolderUri, width, height, fps, bitrate, startSec, endSec);
        } else {
            layoutDownloadProgress.setVisibility(android.view.View.GONE);
            layoutExportProgress.setVisibility(android.view.View.VISIBLE);
            txtExportProgress.setText(getString(R.string.export_error_no_track));
            btnCancel.setText(getString(R.string.export_btn_close));
        }
    }

    private void startExport(android.net.Uri audioUri, String themeJson, String trackName, String outputFolderUri, int width, int height, int fps, int bitrate, int startSec, int endSec) {
        isExporting = true;
        // Разгоняем бар до 10 000 ступеней для максимальной плавности
        downloadProgressBar.setMax(10000);
        exportProgressBar.setMax(10000);

        exportTask = new VideoExportTask(this, trackName, outputFolderUri, width, height, fps, bitrate, startSec, endSec, new VideoExportTask.ExportListener() {
            @Override
            public void onDownloadProgress(long downloadedBytes, long totalBytes) {
                runOnUiThread(() -> {
                    if (layoutDownloadProgress.getVisibility() != android.view.View.VISIBLE) {
                        layoutDownloadProgress.setVisibility(android.view.View.VISIBLE);
                        layoutExportProgress.setVisibility(android.view.View.GONE);
                    }
                    long downloadedMB = downloadedBytes / (1024 * 1024);
                    long totalMB = totalBytes > 0 ? (totalBytes / (1024 * 1024)) : 0;
                    String totalStr = totalMB > 0 ? String.valueOf(totalMB) : "?";
                    txtDownloadProgress.setText(getString(R.string.export_download_progress_format, getString(R.string.export_status_downloading).replace("...", ": "), downloadedMB, totalStr));

                    if (totalBytes > 0) {
                        int percent = (int) ((downloadedBytes * 100) / totalBytes);
                        int targetScaled = percent * 100;
                        if (targetScaled >= targetProgressForBar + 100 || percent == 100) {
                            targetProgressForBar = targetScaled;
                            if (progressAnimator != null && progressAnimator.isRunning()) {
                                progressAnimator.cancel();
                            }
                            progressAnimator = ValueAnimator.ofInt(currentDisplayedProgress, targetScaled);
                            progressAnimator.setDuration(250);
                            progressAnimator.addUpdateListener(animation -> {
                                int val = (int) animation.getAnimatedValue();
                                currentDisplayedProgress = val;
                                downloadProgressBar.setProgress(val);
                            });
                            progressAnimator.start();
                        }
                    }
                });
            }

            @Override
            public void onProgress(int percent) {
                runOnUiThread(() -> {
                    if (layoutExportProgress.getVisibility() != android.view.View.VISIBLE) {
                        layoutDownloadProgress.setVisibility(android.view.View.GONE);
                        layoutExportProgress.setVisibility(android.view.View.VISIBLE);
                        currentDisplayedProgress = 0;
                        targetProgressForBar = 0;
                        exportProgressBar.setProgress(0);
                    }
                    
                    // Update text immediately
                    txtExportProgress.setText(getString(R.string.export_status_rendering).replace("...", ": ") + percent + "%");

                    // Переводим % в шкалу от 0 до 10000
                    int targetScaled = percent * 100;

                    // Animate bar only in steps of 10% (1000 scaled) or at the end for the jelly effect
                    if (targetScaled >= targetProgressForBar + 100 || percent == 100) {
                        targetProgressForBar = targetScaled;

                        if (progressAnimator != null && progressAnimator.isRunning()) {
                            progressAnimator.cancel();
                        }

                        // Анимируем в высоком разрешении (0...10000)
                        progressAnimator = ValueAnimator.ofInt(currentDisplayedProgress, targetScaled);
                        progressAnimator.setDuration(4000);
                        progressAnimator.setInterpolator(new OvershootInterpolator(1.5f)); // Твой жесткий jelly bounce
                        progressAnimator.addUpdateListener(animation -> {
                            int val = (int) animation.getAnimatedValue();
                            currentDisplayedProgress = val;
                            exportProgressBar.setProgress(val);
                        });
                        progressAnimator.start();
                    }
                });
            }

            @Override
            public void onFinished(String path) {
                isExporting = false;
                // Notify MediaScanner so TikTok/Gallery sees the file immediately
                android.media.MediaScannerConnection.scanFile(
                    ExportVideoActivity.this,
                    new String[]{path},
                    new String[]{"video/mp4"},
                    null
                );
                runOnUiThread(() -> {
                    // Ensure the progress races to 100 smoothly if it's not there yet
                    if (progressAnimator != null && progressAnimator.isRunning()) {
                        progressAnimator.cancel();
                    }
                    if (currentDisplayedProgress < 100) {
                        progressAnimator = ValueAnimator.ofInt(currentDisplayedProgress, 100);
                        progressAnimator.setDuration(250); // fast catchup
                        progressAnimator.addUpdateListener(animation -> {
                            int val = (int) animation.getAnimatedValue();
                            currentDisplayedProgress = val;
                            exportProgressBar.setProgress(val);
                            txtExportProgress.setText(val + "%");
                        });
                        progressAnimator.start();
                    } else {
                        exportProgressBar.setProgress(100);
                        txtExportProgress.setText("100%");
                    }

                    // Smooth fade out / fade in for the final text
                    txtExportProgress.animate().alpha(0f).setDuration(250).withEndAction(() -> {
                        txtExportProgress.setText(getString(R.string.export_status_finished) + "\n" + path);
                        txtExportProgress.animate().alpha(1f).setDuration(400).start();
                    }).start();

                    btnCancel.setText(getString(R.string.export_btn_close));
                });
            }
            
            @Override
            public void onError(String error) {
                isExporting = false;
                runOnUiThread(() -> {
                    if (progressAnimator != null && progressAnimator.isRunning()) {
                        progressAnimator.cancel();
                    }
                    layoutDownloadProgress.setVisibility(android.view.View.GONE);
                    layoutExportProgress.setVisibility(android.view.View.VISIBLE);
                    txtExportProgress.setText(getString(R.string.export_error_prefix) + " " + error);
                    btnCancel.setText(getString(R.string.export_btn_close));
                });
            }
        });
        exportTask.start(audioUri, themeJson);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        isExporting = false;
        if (exportTask != null) {
            exportTask.cancel();
        }
    }
}
