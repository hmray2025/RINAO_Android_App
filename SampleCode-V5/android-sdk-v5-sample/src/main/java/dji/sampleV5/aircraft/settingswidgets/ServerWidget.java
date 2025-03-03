package dji.sampleV5.aircraft.settingswidgets;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import dji.sampleV5.modulecommon.util.ITuskServiceCallback;
import dji.v5.ux.core.base.widget.ConstraintLayoutWidget;

public class ServerWidget extends ConstraintLayoutWidget<Object> {
    private SharedPreferences sharedPreferences;
    private String url;
    private ITuskServiceCallback tuskManager;
    private boolean InterfaceBinded = false;
    private Button reconnect_ws_settings;
    private TextView serverip;
    private TextView serverconnected;
    private EditText editTextIP;
    private Button setIP;


    public ServerWidget(@NonNull Context context) {
        super(context);
    }

    public ServerWidget(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ServerWidget(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super.initView(context);
//        sharedPreferences = context.getSharedPreferences("Server_preferences", Context.MODE_PRIVATE);
        inflate(context, dji.v5.ux.R.layout.uxsdk_widget_server, this);
        reconnect_ws_settings = findViewById(dji.v5.ux.R.id.reconnect_ws_settings);
        serverip = findViewById(dji.v5.ux.R.id.serverip);
        serverconnected = findViewById(dji.v5.ux.R.id.serverconnected);
        editTextIP = findViewById(dji.v5.ux.R.id.editTextIP);
        setIP = findViewById(dji.v5.ux.R.id.setIP);

        reconnect_ws_settings.setOnClickListener(v -> {
            tuskManager.callReconnectWebsocket();
            Toast.makeText(this.getContext(), "Reconnecting WS with IP:\n" + tuskManager.callGetIP(), Toast.LENGTH_SHORT).show();
            updateData();
        });
        setIP.setOnClickListener(v -> {
            tuskManager.callSetIP(editTextIP.getText().toString());
            updateData();
        });

        editTextIP.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // not needed, keep here
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                setIP.setEnabled(!s.toString().isEmpty());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // not needed, keep here
            }
        });
    }

    public void updateData() {
        serverip.setText(String.format("Server IP: %s", tuskManager.callGetIP()));
        serverconnected.setText(String.format("Server Connected: %s", tuskManager.callGetConnectionStatus()));
    }

    @Override
    protected void reactToModelChanges() {

    }

    public void setServerManager(ITuskServiceCallback tuskManager) {
        this.tuskManager = tuskManager;
        if (tuskManager != null) {
            InterfaceBinded = true;
        }
    }

    public boolean isInterfaceBinded() {
        return InterfaceBinded;
    }
}
