package dji.v5.ux.pachWidget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;

import dji.v5.ux.R;
import dji.v5.ux.core.base.widget.ConstraintLayoutWidget;
/**
 * PachWidget displays the state of the SAFARI system, including
 * the connection status to the SAFARI system and the flight state
 * of the drone (whether it is autonomous or manual), and actions
 * associated with the state of the drone.
 */

public class PachWidget extends ConstraintLayoutWidget<Object> {
    private PachWidgetModel pachWidgetModel;
    private IPachWidgetModel pach;
    private TextView msg;
    private ImageView connection;
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
        msg.setSelected(true);
    }

    @Override
    protected void reactToModelChanges() {
        pachWidgetModel.msgdata.observe((LifecycleOwner) getContext(), msgData -> {
            msg.setText(msgData);
        });
        pachWidgetModel.connectiondata.observe((LifecycleOwner) getContext(), connected -> {
            if (connected) {
                connection.setImageResource(R.drawable.uxsdk_ic_alert_good);
            } else if (!connected) {
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
