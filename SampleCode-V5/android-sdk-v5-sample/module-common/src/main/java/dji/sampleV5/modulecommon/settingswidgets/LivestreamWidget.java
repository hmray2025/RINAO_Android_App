package dji.sampleV5.modulecommon.settingswidgets;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import dji.sampleV5.modulecommon.R;
import dji.sampleV5.modulecommon.util.IStreamManager;
import dji.sampleV5.modulecommon.util.ITuskServiceCallback;
import dji.v5.ux.core.base.widget.ConstraintLayoutWidget;

public class LivestreamWidget extends ConstraintLayoutWidget<Object> {
    private ITuskServiceCallback tuskService;
    private IStreamManager streamManager;
    private Button HD1080;
    private Button HD720;
    private Button SD540;
    public LivestreamWidget(@NonNull Context context) {
        super(context);
        inflate(context, dji.v5.ux.R.layout.uxsdk_widget_sartopo, this);
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
        HD1080 = findViewById(dji.v5.ux.R.id.HD1080);
        HD720 = findViewById(dji.v5.ux.R.id.HD720);
        SD540 = findViewById(dji.v5.ux.R.id.SD540);

        HD1080.setOnClickListener(v -> {
            streamManager.setStreamQuality(IStreamManager.StreamQuality.FULL_HD);
            setStreamSelection();
        });

        HD720.setOnClickListener(v -> {
            streamManager.setStreamQuality(IStreamManager.StreamQuality.HD);
            setStreamSelection();
        });

        SD540.setOnClickListener(v -> {
            streamManager.setStreamQuality(IStreamManager.StreamQuality.SD);
            setStreamSelection();
        });
    }

    @Override
    protected void reactToModelChanges() {

    }

    private void setStreamSelection() {
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
    }

    private void setTuskService(ITuskServiceCallback tuskService) {
        this.tuskService = tuskService;
    }

    private void setStreamManager(IStreamManager streamManager) {
        this.streamManager = streamManager;
    }
}
