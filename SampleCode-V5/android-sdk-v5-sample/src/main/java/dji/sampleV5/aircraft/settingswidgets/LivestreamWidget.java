package dji.sampleV5.aircraft.settingswidgets;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.MessageFormat;

import dji.sampleV5.aircraft.R;
import dji.sampleV5.aircraft.util.IStreamManager;
import dji.v5.manager.datacenter.livestream.StreamQuality;
import dji.v5.ux.core.base.widget.ConstraintLayoutWidget;

public class LivestreamWidget extends ConstraintLayoutWidget<Object> {
    private IStreamManager streamManager;
    private Button HD1080;
    private Button HD720;
    private Button SD540;
    private TextView livestream_bitrate;
    private TextView stream_quality;
    private TextView currently_streaming;
    private TextView livestream_url;
    private boolean InterfaceBinded = false;
    public LivestreamWidget(@NonNull Context context) {
        super(context);
    }

    public LivestreamWidget(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public LivestreamWidget(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super.initView(context);
        inflate(context, dji.v5.ux.R.layout.uxsdk_widget_livestream, this);
        HD1080 = findViewById(dji.v5.ux.R.id.lHD1080);
        HD720 = findViewById(dji.v5.ux.R.id.lHD720);
        SD540 = findViewById(dji.v5.ux.R.id.lSD540);
        livestream_bitrate = findViewById(dji.v5.ux.R.id.livestream_bitrate);
        stream_quality = findViewById(dji.v5.ux.R.id.stream_quality);
        currently_streaming = findViewById(dji.v5.ux.R.id.currently_streaming);
        livestream_url = findViewById(dji.v5.ux.R.id.livestream_url);
        HD1080.setOnClickListener(v -> {
            streamManager.setStreamQuality(StreamQuality.FULL_HD);
            setStreamSelection();
        });

        HD720.setOnClickListener(v -> {
            streamManager.setStreamQuality(StreamQuality.HD);
            setStreamSelection();
        });

        SD540.setOnClickListener(v -> {
            streamManager.setStreamQuality(StreamQuality.SD);
            setStreamSelection();
        });
    }

    public void updateFields() {
        livestream_bitrate.setText(MessageFormat.format("Bitrate: {0}", streamManager.getBitrate()));
        stream_quality.setText(MessageFormat.format("Stream Quality: {0}", streamManager.getStreamQuality()));
        currently_streaming.setText(MessageFormat.format("Currently Streaming: {0}", streamManager.isStreaming()));
        livestream_url.setText(MessageFormat.format("Livestream URL: {0}", streamManager.getStreamURL()));
    }

    @Override
    protected void reactToModelChanges() {

    }

    public void setStreamSelection() {
        Button[] buttons = new Button[]{HD1080, HD720, SD540};
        int selectionIndex = -1;

        switch (streamManager.getStreamQuality()) {
            case FULL_HD:
                selectionIndex = 0;
                break;
            case HD:
                selectionIndex = 1;
                break;
            case SD:
                selectionIndex = 2;
                break;
        }

        for (int index = 0; index < buttons.length; index++) {
            if (index == selectionIndex) {
                buttons[index].setBackgroundResource(R.drawable.rounded_selected);
            } else {
                buttons[index].setBackgroundResource(R.drawable.rounded_white_bg);
            }
        }
        updateFields();
    }

    public void setStreamManager(IStreamManager streamManager) {
        this.streamManager = streamManager;
        if (streamManager != null) {
            InterfaceBinded = true;
        }
    }

    public boolean isInterfaceBinded() {
        return InterfaceBinded;
    }
}
