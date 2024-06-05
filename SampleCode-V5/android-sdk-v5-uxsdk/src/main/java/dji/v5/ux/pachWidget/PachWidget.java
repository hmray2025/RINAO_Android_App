package dji.v5.ux.pachWidget;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;

import dji.v5.ux.R;
import dji.v5.ux.core.base.DJISDKModel;
import dji.v5.ux.core.base.widget.ConstraintLayoutWidget;
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore;
import io.reactivex.BackpressureStrategy;
import io.reactivex.rxjava3.core.Flowable;

public class PachWidget extends ConstraintLayoutWidget<Object> {
    private PachWidgetModel pachWidgetModel;
    private IPachWidgetModel pach;
    private TextView msg;
    private ImageView connection;
//    private Flowable<String> msgDataFlowable;
    public PachWidget(@NonNull Context context) {
        super(context);
    }

    public PachWidget(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public PachWidget(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        inflate(context, R.layout.uxsdk_widget_pach, this);
        pachWidgetModel = pachWidgetModel.getInstance();
        msg = findViewById(R.id.status_msg);
        connection = findViewById(R.id.connection_icon);
//        msgDataFlowable = Flowable.create(emitter -> {
//            pachWidgetModel.getMsgData().observe((LifecycleOwner) context, msgData -> {
//                if (!emitter.isCancelled()) {
//                    emitter.onNext(msgData);
//                }
//            });
//        }, BackpressureStrategy.BUFFER);
    }

    @Override
    protected void reactToModelChanges() {
        pachWidgetModel.msgdata.observe((LifecycleOwner) getContext(), msgData -> {
            msg.setText(msgData);
        });
        pachWidgetModel.connectiondata.observe((LifecycleOwner) getContext(), connectionData -> {
            if (connectionData) {
                connection.setImageResource(R.drawable.uxsdk_ic_alert_good);
            } else {
                connection.setImageResource(R.drawable.uxsdk_ic_alert_error);
            }
        });
    }

    public void onCreate(IPachWidgetModel pach) {
        this.pach = pach;
        if (this.pach != null) {
            pachWidgetModel.setPach(this.pach);
        }
    }
}
