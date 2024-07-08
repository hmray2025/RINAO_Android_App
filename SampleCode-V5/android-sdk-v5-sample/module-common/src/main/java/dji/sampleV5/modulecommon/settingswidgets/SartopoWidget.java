package dji.sampleV5.modulecommon.settingswidgets;

import static android.app.PendingIntent.getActivity;
import static android.provider.Settings.System.getString;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import dji.v5.ux.R;
import dji.v5.ux.core.base.widget.ConstraintLayoutWidget;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;

public class SartopoWidget extends ConstraintLayoutWidget<Object> {
    /*
    This class takes in user input within the settings panel to set the
    Shared Preferences values for the SARTopo Access URL, Device ID, and base URL.
    It is indended to only update these parameters. PachKeyManager will handle sending
    the updated URL values via get requests to the SARTopo API.
     */
    private SharedPreferences sharedPreferences;
    private String base_url;
    private String access_url;
    private String device_id;
    private ISartopoWidgetModel sartopoWidgetModel;

    public SartopoWidget(@NonNull Context context) {
        super(context);
    }

    public SartopoWidget(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public SartopoWidget(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super.initView(context);
//        dataStore = new RxPreferenceDataStoreBuilder(context, /*name=*/ "sartopo_settings").build();
        sharedPreferences = context.getSharedPreferences("SARTopo_preferences",Context.MODE_PRIVATE);
        inflate(context, R.layout.uxsdk_widget_sartopo, this);

        EditText editAccess = findViewById(R.id.edit_access_url);
        EditText editID = findViewById(R.id.edit_device_id);
        EditText editBase = findViewById(R.id.edit_base_url);
        TextView url = findViewById(R.id.sartopo_url);

        try {
            base_url = retrieveString("base_url");
            access_url = retrieveString("access_url");
            device_id = retrieveString("device_id");
            sartopoWidgetModel.setBaseURL(base_url);
            sartopoWidgetModel.setAccessURL(access_url);
            sartopoWidgetModel.setDeviceID(device_id);
        } catch (Exception e) {
            Log.e("SartopoWidget", "Error retrieving values from SharedPreferences: " + e.getMessage());
            base_url = "Not set";
            access_url = "Not set";
            device_id = "Not set";
        }
        url.setText(String.format("%s%s?id=%s&lat={LAT}&lng={LNG}", base_url, access_url, device_id));

        editAccess.setText(retrieveString("access_url")); // Use the same key for saving and retrieving
        editAccess.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String text = s.toString();
                // Save the text to SharedPreferences
                saveString("access_url", text);
                // Assuming sartopoWidgetModel is not null and properly initialized
                if (sartopoWidgetModel != null) {
                    sartopoWidgetModel.setAccessURL(text);
                    base_url = sartopoWidgetModel.getBaseURL();
                    access_url = sartopoWidgetModel.getAccessURL();
                    device_id = sartopoWidgetModel.getDeviceID();
                    url.setText(String.format("%s%s?id=%s&lat={LAT}&lng={LNG}", base_url, access_url, device_id));
                }
            }
        });
        editID.setText(retrieveString("device_id")); // Use the same key for saving and retrieving
        editID.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String text = s.toString();
                // Save the text to SharedPreferences
                saveString("device_id", text);
                // Assuming sartopoWidgetModel is not null and properly initialized
                if (sartopoWidgetModel != null) {
                    sartopoWidgetModel.setDeviceID(text);
                    base_url = sartopoWidgetModel.getBaseURL();
                    access_url = sartopoWidgetModel.getAccessURL();
                    device_id = sartopoWidgetModel.getDeviceID();
                    url.setText(String.format("%s%s?id=%s&lat={LAT}&lng={LNG}", base_url, access_url, device_id));
                }
            }
        });
        editBase.setText(retrieveString("base_url")); // Use the same key for saving and retrieving
        editBase.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String text = s.toString();
                // Save the text to SharedPreferences
                saveString("base_url", text);
                // Assuming sartopoWidgetModel is not null and properly initialized
                if (sartopoWidgetModel != null) {
                    sartopoWidgetModel.setBaseURL(text);
                    base_url = sartopoWidgetModel.getBaseURL();
                    access_url = sartopoWidgetModel.getAccessURL();
                    device_id = sartopoWidgetModel.getDeviceID();
                    url.setText(String.format("%s%s?id=%s&lat={LAT}&lng={LNG}", base_url, access_url, device_id));

                }
            }
        });
//        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
//            @Override
//            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
//                if (actionId == EditorInfo.IME_ACTION_DONE) {
//                    // Update your text variable here
//                    String updatedText = editText.getText().toString();
//                    // Do something with the updatedText
//                    saveString("SARTopoAccessURL", editText.getText().toString()); // Save the text after any change
//                    return true; // Consume the action
//                }
//                return false; // Pass on to other listeners.
//            }
//        });
    }

    @Override
    protected void reactToModelChanges() {

    }

    // Method to save string to SharedPreferences
    public void saveString(String key, String value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

//    public void saveString(String key, String value) {
//        Preferences.Key<String> preferencesKey = PreferencesKeys.stringKey(key);
//        Disposable savestr = dataStore.updateDataAsync(prefsIn -> {
//            MutablePreferences mutablePreferences = prefsIn.toMutablePreferences();
//            mutablePreferences.set(preferencesKey, value);
//            return Single.just(mutablePreferences);
//        }).subscribe(
//                prefsOut -> {
//                    // success, no action needed
//                },
//                err -> {
//                    Log.e("SartopoWidget", "Error saving string to DataStore: " + err.getMessage());
//                }
//        );
//    }

    // Method to retrieve string from SharedPreferences
    public String retrieveString(String key) {
        return sharedPreferences.getString(key, "Not set");
    }

    public void setSartopoWidgetModel(ISartopoWidgetModel sartopoWidgetModel) {
        this.sartopoWidgetModel = sartopoWidgetModel;
    }
}
