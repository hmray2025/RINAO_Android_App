/*
 * Copyright (c) 2018-2020 DJI
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package dji.sampleV5.aircraft.fpvlayout;

import static com.google.android.gms.common.util.CollectionUtils.listOf;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.core.view.GravityCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import dji.sampleV5.aircraft.control.PachKeyManager;
import dji.sampleV5.aircraft.video.StreamManager;
import dji.sdk.keyvalue.value.common.CameraLensType;
import dji.sdk.keyvalue.value.common.ComponentIndexType;
import dji.v5.common.video.channel.VideoChannelType;
import dji.v5.common.video.interfaces.IVideoChannel;
import dji.v5.manager.interfaces.ICameraStreamManager;
import dji.v5.network.DJINetworkManager;
import dji.v5.network.IDJINetworkStatusListener;
import dji.v5.manager.datacenter.MediaDataCenter;
import dji.v5.utils.common.JsonUtil;
import dji.v5.utils.common.LogPath;
import dji.v5.utils.common.LogUtils;
import dji.v5.ux.R;
import dji.v5.ux.accessory.RTKStartServiceHelper;
import dji.v5.ux.cameracore.widget.autoexposurelock.AutoExposureLockWidget;
import dji.v5.ux.cameracore.widget.cameracontrols.CameraControlsWidget;
import dji.v5.ux.cameracore.widget.cameracontrols.exposuresettings.ExposureSettingsPanel;
import dji.v5.ux.cameracore.widget.cameracontrols.lenscontrol.LensControlWidget;
import dji.v5.ux.cameracore.widget.focusexposureswitch.FocusExposureSwitchWidget;
import dji.v5.ux.cameracore.widget.focusmode.FocusModeWidget;
import dji.v5.ux.cameracore.widget.fpvinteraction.FPVInteractionWidget;
import dji.v5.ux.core.base.SchedulerProvider;
import dji.v5.ux.core.communication.BroadcastValues;
import dji.v5.ux.core.extension.ViewExtensions;
import dji.v5.ux.core.panel.systemstatus.SystemStatusListPanelWidget;
import dji.v5.ux.core.panel.topbar.TopBarPanelWidget;
import dji.v5.ux.core.util.CameraUtil;
import dji.v5.ux.core.util.CommonUtils;
import dji.v5.ux.core.util.DataProcessor;
import dji.v5.ux.core.widget.fpv.FPVWidget;
import dji.v5.ux.core.widget.hsi.HorizontalSituationIndicatorWidget;
import dji.v5.ux.core.widget.hsi.PrimaryFlightDisplayWidget;
import dji.v5.ux.core.widget.setting.SettingPanelWidget;
import dji.v5.ux.gimbal.GimbalFineTuneWidget;
import dji.v5.ux.core.widget.setting.SettingWidget;
import dji.v5.ux.core.widget.simulator.SimulatorIndicatorWidget;
import dji.v5.ux.core.widget.systemstatus.SystemStatusWidget;
import dji.v5.ux.map.MapWidget;
import dji.v5.ux.mapkit.core.maps.DJIUiSettings;
import dji.v5.ux.pachWidget.PachWidget;
import dji.v5.ux.training.simulatorcontrol.SimulatorControlWidget;
import dji.v5.ux.visualcamera.CameraNDVIPanelWidget;
import dji.v5.ux.visualcamera.CameraVisiblePanelWidget;
import dji.v5.ux.visualcamera.zoom.FocalZoomWidget;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import dji.sampleV5.aircraft.BuildConfig;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import dji.v5.ux.mapkit.core.maps.DJIMap;


/**
 * Displays a sample layout of widgets similar to that of the various DJI apps.
 */
public class FPVLayoutActivity extends AppCompatActivity {

    //region Fields
    private final String TAG = LogUtils.getTag(this);

    protected FPVWidget primaryFpvWidget;
    protected FPVInteractionWidget fpvInteractionWidget;
    protected FPVWidget secondaryFPVWidget;
    protected SystemStatusListPanelWidget systemStatusListPanelWidget;
    protected SimulatorControlWidget simulatorControlWidget;
    protected LensControlWidget lensControlWidget;
    protected AutoExposureLockWidget autoExposureLockWidget;
    protected FocusModeWidget focusModeWidget;
    protected FocusExposureSwitchWidget focusExposureSwitchWidget;
    protected CameraControlsWidget cameraControlsWidget;
    protected HorizontalSituationIndicatorWidget horizontalSituationIndicatorWidget;
    protected ExposureSettingsPanel exposureSettingsPanel;
    protected PrimaryFlightDisplayWidget pfvFlightDisplayWidget;
    protected CameraNDVIPanelWidget ndviCameraPanel;
//    protected CameraVisiblePanelWidget visualCameraPanel;
    protected FocalZoomWidget focalZoomWidget;
    protected SettingWidget settingWidget;
    protected MapWidget mapWidget;
    protected TopBarPanelWidget topBarPanel;
    protected StreamManager streamManager;
    protected PachWidget pachWidget;
    private DrawerLayout mDrawerLayout;
    private ComponentIndexType lastDevicePosition = ComponentIndexType.UNKNOWN;
    private CameraLensType lastLensType = CameraLensType.UNKNOWN;
    private CompositeDisposable compositeDisposable;
    private final DataProcessor<CameraSource> cameraSourceProcessor = DataProcessor.create(new CameraSource(ComponentIndexType.UNKNOWN,
            CameraLensType.UNKNOWN));
    private Button liveStreamButton;
    private Button mapExpand;
    private PachKeyManager pachManager;
    private GimbalFineTuneWidget gimbalFineTuneWidget;
    private Button mMediaManagerBtn;

    private final IDJINetworkStatusListener networkStatusListener = isNetworkAvailable -> {
        if (isNetworkAvailable) {
            LogUtils.d(TAG, "isNetworkAvailable=" + true);
            RTKStartServiceHelper.INSTANCE.startRtkService(false);
        }
    };
    private final ICameraStreamManager.AvailableCameraUpdatedListener availableCameraUpdatedListener = availableCameraList -> {
        runOnUiThread(() -> updateFPVWidgetSource(availableCameraList));
    };

    //endregion

    //region Lifecycle
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.uxsdk_activity_default_layout);
        Intent intent = getIntent();
        mDrawerLayout = findViewById(R.id.root_view);
        topBarPanel = findViewById(R.id.panel_top_bar);
        settingWidget = topBarPanel.getSettingWidget();
        primaryFpvWidget = findViewById(R.id.widget_primary_fpv);
        fpvInteractionWidget = findViewById(R.id.widget_fpv_interaction);
        secondaryFPVWidget = findViewById(R.id.widget_secondary_fpv);
        systemStatusListPanelWidget = findViewById(R.id.widget_panel_system_status_list);
        simulatorControlWidget = findViewById(R.id.widget_simulator_control);
        lensControlWidget = findViewById(R.id.widget_lens_control);
        ndviCameraPanel = findViewById(R.id.panel_ndvi_camera);
//        visualCameraPanel = findViewById(R.id.panel_visual_camera);
        mDrawerLayout = findViewById(R.id.root_view);
        autoExposureLockWidget = findViewById(R.id.widget_auto_exposure_lock);
        focusModeWidget = findViewById(R.id.widget_focus_mode);
        focusExposureSwitchWidget = findViewById(R.id.widget_focus_exposure_switch);
        exposureSettingsPanel = findViewById(R.id.panel_camera_controls_exposure_settings);
        pfvFlightDisplayWidget = findViewById(R.id.widget_fpv_flight_display_widget);
        focalZoomWidget = findViewById(R.id.widget_focal_zoom);
        cameraControlsWidget = findViewById(R.id.widget_camera_controls);
        horizontalSituationIndicatorWidget = findViewById(R.id.widget_horizontal_situation_indicator);
        pachWidget = findViewById(R.id.pach_widget);
        mapWidget = findViewById(R.id.widget_map);
        cameraControlsWidget.getExposureSettingsIndicatorWidget().setStateChangeResourceId(R.id.panel_camera_controls_exposure_settings);
        gimbalFineTuneWidget = findViewById(R.id.setting_menu_gimbal_fine_tune);
//        ViewStub stub = findViewById(R.id.manual_right_nav_setting_stub);
//        if (stub != null) {
//            mSettingPanelWidget = (SettingPanelWidget) stub.inflate();
//        }
        mapExpand = findViewById(R.id.button_expand_map);
        liveStreamButton = findViewById(R.id.myLiveStream);

        initClickListener();
        MediaDataCenter.getInstance().getCameraStreamManager().addAvailableCameraUpdatedListener(availableCameraUpdatedListener);
        primaryFpvWidget.setOnFPVStreamSourceListener((devicePosition, lensType) -> {
            cameraSourceProcessor.onNext(new CameraSource(devicePosition, lensType));
        });

        //小surfaceView放置在顶部，避免被大的遮挡
        secondaryFPVWidget.setSurfaceViewZOrderOnTop(true);
        secondaryFPVWidget.setSurfaceViewZOrderMediaOverlay(true);

//        mapWidget.initMapLibreMap(getApplicationContext(), map -> {
//            DJIUiSettings uiSetting = map.getUiSettings();
//            if (uiSetting != null) {
//                uiSetting.setZoomControlsEnabled(false);//hide zoom widget
//            }
//        });

        mapWidget.initMapLibreMap(getApplicationContext(), map ->
                map.setMapType(DJIMap.MapType.NORMAL)
        );

        mapWidget.onCreate(savedInstanceState);
        pachManager = PachKeyManager.Companion.getInstance(); // points to object created in DJIAircraftMainActivity
        streamManager = pachManager.getStreamer(); // sets the streamManager to the streamer in Pach
    }

    private void isGimableAdjustClicked(BroadcastValues broadcastValues) {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.END)) {
            mDrawerLayout.closeDrawers();
        }
        horizontalSituationIndicatorWidget.setVisibility(View.GONE);
        if (gimbalFineTuneWidget != null) {
            gimbalFineTuneWidget.setVisibility(View.VISIBLE);
        }
    }

    private void initClickListener() {
        secondaryFPVWidget.setOnClickListener(v -> swapVideoSource());
//        streamManager.initChannelStateListener();


        if (settingWidget != null) {
            settingWidget.setOnClickListener(v -> toggleRightDrawer());
        }

        // Setup top bar state callbacks
        SystemStatusWidget systemStatusWidget = topBarPanel.getSystemStatusWidget();
        if (systemStatusWidget != null) {
            systemStatusWidget.setOnClickListener(v -> ViewExtensions.toggleVisibility(systemStatusListPanelWidget));
        }

        SimulatorIndicatorWidget simulatorIndicatorWidget = topBarPanel.getSimulatorIndicatorWidget();
        if (simulatorIndicatorWidget != null) {
            simulatorIndicatorWidget.setOnClickListener(v -> ViewExtensions.toggleVisibility(simulatorControlWidget));
        }

        if (liveStreamButton != null) {
            liveStreamButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    streamManager.startStream();
//                    sendStreamURL(streamManager.getStreamURL())
                    pachManager.sendStreamURL(streamManager.getStreamURL());
                    if (streamManager.isStreaming()) {
                        liveStreamButton.setBackgroundResource(R.drawable.uxsdk_livestream_stop);
                        pachManager.sendStreamDataToServer(true);
                    }
                    else {
                        liveStreamButton.setBackgroundResource(R.drawable.uxsdk_livestream_start);
                        pachManager.sendStreamDataToServer(false);
                    }
                }
            });
        }
        if (mapExpand != null && mapWidget != null) {
            mapExpand.setOnClickListener(new View.OnClickListener() {
                final int mapWidth = getResources().getDimensionPixelSize(R.dimen.uxsdk_150_dp);
                final int mapHeight = getResources().getDimensionPixelSize(R.dimen.uxsdk_100_dp);
                @Override
                public void onClick(View view) {
                    ViewGroup.LayoutParams params = mapWidget.getLayoutParams();
                    if (params.width == mapWidth) {
                        params.height = primaryFpvWidget.getHeight() - (topBarPanel.getBottom() + getResources().getDimensionPixelSize(R.dimen.uxsdk_12_dp));
                        params.width = primaryFpvWidget.getWidth() - getResources().getDimensionPixelSize(R.dimen.uxsdk_24_dp);
                        mapExpand.setBackgroundResource(R.drawable.arrow_line_down_right);
                    }
                    else {
                        params.height = mapHeight;
                        params.width = mapWidth;
                        mapExpand.setBackgroundResource(R.drawable.arrow_line_up_left);
                    }
                    mapWidget.setLayoutParams(params);
                }
            });
        }
    }

    private void toggleRightDrawer() {
        mDrawerLayout.openDrawer(GravityCompat.END);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapWidget.onDestroy();
        MediaDataCenter.getInstance().getCameraStreamManager().removeAvailableCameraUpdatedListener(availableCameraUpdatedListener);
        DJINetworkManager.getInstance().removeNetworkStatusListener(networkStatusListener);
//        removeChannelStateListener();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapWidget.onResume();
        compositeDisposable = new CompositeDisposable();
        mapWidget.subscribeToDataSource(pachManager.getWaypointFlowable()); // connects the mapWidget to the dataFlowable in PachKeyManager
        pachWidget.subscribetoDataSources(
                pachManager.getConnectionFlowable(),
                pachManager.getMessageFlowable()); // connects the pachWidget to the dataFlowable in PachKeyManager
        compositeDisposable.add(systemStatusListPanelWidget.closeButtonPressed()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(pressed -> {
                    if (pressed) {
                        ViewExtensions.hide(systemStatusListPanelWidget);
                    }
                }));
        compositeDisposable.add(simulatorControlWidget.getUIStateUpdates()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(simulatorControlWidgetState -> {
                    if (simulatorControlWidgetState instanceof SimulatorControlWidget.UIState.VisibilityUpdated) {
                        if (((SimulatorControlWidget.UIState.VisibilityUpdated) simulatorControlWidgetState).isVisible()) {
                            hideOtherPanels(simulatorControlWidget);
                        }
                    }
                }));
        compositeDisposable.add(cameraSourceProcessor.toFlowable()
                .observeOn(SchedulerProvider.io())
                .throttleLast(500, TimeUnit.MILLISECONDS)
                .subscribeOn(SchedulerProvider.io())
                .subscribe(result -> runOnUiThread(() -> onCameraSourceUpdated(result.devicePosition, result.lensType)))
        );
        compositeDisposable.add(pachManager.getWaypointFlowable()
                .observeOn(SchedulerProvider.io())
                .subscribeOn(SchedulerProvider.io())
                .subscribe(result -> Log.d("JAKEDEBUG2", "pachwidget data: " + result))

        );
        if (streamManager.isStreaming()) {
            liveStreamButton.setBackgroundResource(R.drawable.uxsdk_livestream_stop);
            pachManager.sendStreamDataToServer(true);
        }
        else {
            liveStreamButton.setBackgroundResource(R.drawable.uxsdk_livestream_start);
            pachManager.sendStreamDataToServer(false);

        }
    }

    @Override
    protected void onPause() {
        if (compositeDisposable != null) {
            compositeDisposable.dispose();
            compositeDisposable = null;
        }
        mapWidget.onPause();
        super.onPause();
    }
    //endregion

    private void hideOtherPanels(@Nullable View widget) {
        View[] panels = {
                simulatorControlWidget
        };

        for (View panel : panels) {
            if (widget != panel) {
                panel.setVisibility(View.GONE);
            }
        }
    }

    private void updateFPVWidgetSource(List<ComponentIndexType> availableCameraList) {
        LogUtils.i(TAG, JsonUtil.toJson(availableCameraList));
        if (availableCameraList == null) {
            return;
        }

        ArrayList<ComponentIndexType> cameraList = new ArrayList<>(availableCameraList);

        //No Data available
        if (cameraList.isEmpty()) {
            secondaryFPVWidget.setVisibility(View.GONE);
            return;
        }

        //Only one data channel
        if (cameraList.size() == 1) {
            primaryFpvWidget.updateVideoSource(availableCameraList.get(0));
            secondaryFPVWidget.setVisibility(View.GONE);
            return;
        }

        //More than two data channels
        ComponentIndexType primarySource = getSuitableSource(cameraList, ComponentIndexType.LEFT_OR_MAIN);
        primaryFpvWidget.updateVideoSource(primarySource);
        cameraList.remove(primarySource);

        ComponentIndexType secondarySource = getSuitableSource(cameraList, ComponentIndexType.FPV);
        secondaryFPVWidget.updateVideoSource(secondarySource);

        secondaryFPVWidget.setVisibility(View.VISIBLE);
    }

    private ComponentIndexType getSuitableSource(List<ComponentIndexType> cameraList, ComponentIndexType defaultSource) {
        if (cameraList.contains(ComponentIndexType.LEFT_OR_MAIN)) {
            return ComponentIndexType.LEFT_OR_MAIN;
        } else if (cameraList.contains(ComponentIndexType.RIGHT)) {
            return ComponentIndexType.RIGHT;
        } else if (cameraList.contains(ComponentIndexType.UP)) {
            return ComponentIndexType.UP;
        } else if (cameraList.contains(ComponentIndexType.VISION_ASSIST)) {
            return ComponentIndexType.VISION_ASSIST;
        }
        return defaultSource;
    }

//    private void initChannelStateListener() {
//        ComponentIndexType primarySource = getSuitableSource(cameraList, ComponentIndexType.LEFT_OR_MAIN);
//        primaryChannel =  MediaDataCenter.getInstance().getVideoStreamManager().getAvailableVideoChannel(VideoChannelType.PRIMARY_STREAM_CHANNEL);
//        primaryChannel?.addVideoChannelStateChangeListener(primaryChannelStateListener);
//        IVideoChannel secondaryChannel =
//                MediaDataCenter.getInstance().getVideoStreamManager().getAvailableVideoChannel(VideoChannelType.SECONDARY_STREAM_CHANNEL);
//        if (primaryChannel != null) {
//            primaryChannelStateListener = (from, to) -> {
//                StreamSource primaryStreamSource = primaryChannel.getStreamSource();
//                if (VideoChannelState.ON == to && primaryStreamSource != null) {
//                    runOnUiThread(() -> primaryFpvWidget.updateVideoSource(primarySource));
//                }
//            };
//            primaryChannel.addVideoChannelStateChangeListener(primaryChannelStateListener);
//        }
//        if (secondaryChannel != null) {
//            secondaryChannelStateListener = (from, to) -> {
//                StreamSource secondaryStreamSource = secondaryChannel.getStreamSource();
//                if (VideoChannelState.ON == to && secondaryStreamSource != null) {
//                    runOnUiThread(() -> secondaryFPVWidget.updateVideoSource(secondaryStreamSource, VideoChannelType.SECONDARY_STREAM_CHANNEL));
//                }
//            };
//            secondaryChannel.addVideoChannelStateChangeListener(secondaryChannelStateListener);
//        }
//    }

//    private void removeChannelStateListener() {
//        IVideoChannel primaryChannel =
//                MediaDataCenter.getInstance().getVideoStreamManager().getAvailableVideoChannel(VideoChannelType.PRIMARY_STREAM_CHANNEL);
//        IVideoChannel secondaryChannel =
//                MediaDataCenter.getInstance().getVideoStreamManager().getAvailableVideoChannel(VideoChannelType.SECONDARY_STREAM_CHANNEL);
//        if (primaryChannel != null) {
//            primaryChannel.removeVideoChannelStateChangeListener(primaryChannelStateListener);
//        }
//        if (secondaryChannel != null) {
//            secondaryChannel.removeVideoChannelStateChangeListener(secondaryChannelStateListener);
//        }
//    }

    private void onCameraSourceUpdated(ComponentIndexType devicePosition, CameraLensType lensType) {
        LogUtils.i(LogPath.SAMPLE, "onCameraSourceUpdated", devicePosition, lensType);
        if (devicePosition == lastDevicePosition && lensType == lastLensType) {
            return;
        }
        lastDevicePosition = devicePosition;
        lastLensType = lensType;
//        updateViewVisibility(devicePosition, lensType);
        updateInteractionEnabled();
        //如果无需使能或者显示的，也就没有必要切换了。
        if (fpvInteractionWidget.isInteractionEnabled()) {
            fpvInteractionWidget.updateCameraSource(devicePosition, lensType);
            fpvInteractionWidget.updateGimbalIndex(CommonUtils.getGimbalIndex(devicePosition));
        }
        if (lensControlWidget.getVisibility() == View.VISIBLE) {
            lensControlWidget.updateCameraSource(devicePosition, lensType);
        }
        if (ndviCameraPanel.getVisibility() == View.VISIBLE) {
            ndviCameraPanel.updateCameraSource(devicePosition, lensType);
        }
//        if (visualCameraPanel.getVisibility() == View.VISIBLE) {
//            visualCameraPanel.updateCameraSource(devicePosition, lensType);
//        }
        if (autoExposureLockWidget.getVisibility() == View.VISIBLE) {
            autoExposureLockWidget.updateCameraSource(devicePosition, lensType);
        }
        if (focusModeWidget.getVisibility() == View.VISIBLE) {
            focusModeWidget.updateCameraSource(devicePosition, lensType);
        }
        if (focusExposureSwitchWidget.getVisibility() == View.VISIBLE) {
            focusExposureSwitchWidget.updateCameraSource(devicePosition, lensType);
        }
        if (cameraControlsWidget.getVisibility() == View.VISIBLE) {
            cameraControlsWidget.updateCameraSource(devicePosition, lensType);
        }
        if (focalZoomWidget.getVisibility() == View.VISIBLE) {
            focalZoomWidget.updateCameraSource(devicePosition, lensType);
        }
        if (horizontalSituationIndicatorWidget.getVisibility() == View.VISIBLE) {
            horizontalSituationIndicatorWidget.updateCameraSource(devicePosition, lensType);
        }
    }

    private void updateInteractionEnabled() {
        fpvInteractionWidget.setInteractionEnabled(!CameraUtil.isFPVTypeView(primaryFpvWidget.getWidgetModel().getCameraIndex()));
    }

    private void updateViewVisibility(ComponentIndexType devicePosition, CameraLensType lensType) {
        //只在fpv下显示
        pfvFlightDisplayWidget.setVisibility(CameraUtil.isFPVTypeView(devicePosition) ? View.VISIBLE : View.INVISIBLE);

        //fpv下不显示
        lensControlWidget.setVisibility(CameraUtil.isFPVTypeView(devicePosition) ? View.INVISIBLE : View.VISIBLE);
        ndviCameraPanel.setVisibility(CameraUtil.isFPVTypeView(devicePosition) ? View.INVISIBLE : View.VISIBLE);
//        visualCameraPanel.setVisibility(CameraUtil.isFPVTypeView(devicePosition) ? View.INVISIBLE : View.VISIBLE);
        autoExposureLockWidget.setVisibility(CameraUtil.isFPVTypeView(devicePosition) ? View.INVISIBLE : View.VISIBLE);
        focusModeWidget.setVisibility(CameraUtil.isFPVTypeView(devicePosition) ? View.INVISIBLE : View.VISIBLE);
        focusExposureSwitchWidget.setVisibility(CameraUtil.isFPVTypeView(devicePosition) ? View.INVISIBLE : View.VISIBLE);
        cameraControlsWidget.setVisibility(CameraUtil.isFPVTypeView(devicePosition) ? View.INVISIBLE : View.VISIBLE);
        focalZoomWidget.setVisibility(CameraUtil.isFPVTypeView(devicePosition) ? View.INVISIBLE : View.VISIBLE);
        horizontalSituationIndicatorWidget.setSimpleModeEnable(CameraUtil.isFPVTypeView(devicePosition));

        //只在部分len下显示
        ndviCameraPanel.setVisibility(CameraUtil.isSupportForNDVI(lensType) ? View.VISIBLE : View.INVISIBLE);
    }

    /**
     * Swap the video sources of the FPV and secondary FPV widgets.
     */
    private void swapVideoSource() {
        ComponentIndexType primarySource = primaryFpvWidget.getWidgetModel().getCameraIndex();
        ComponentIndexType secondarySource = secondaryFPVWidget.getWidgetModel().getCameraIndex();
        //两个source都存在的情况下才进行切换
        if (primarySource != ComponentIndexType.UNKNOWN && secondarySource != ComponentIndexType.UNKNOWN) {
            primaryFpvWidget.updateVideoSource(secondarySource);
            secondaryFPVWidget.updateVideoSource(primarySource);
        }
    }


    private static class CameraSource {
        ComponentIndexType devicePosition;
        CameraLensType lensType;

        public CameraSource(ComponentIndexType devicePosition, CameraLensType lensType) {
            this.devicePosition = devicePosition;
            this.lensType = lensType;
        }
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.END)) {
            mDrawerLayout.closeDrawers();
        } else {
            super.onBackPressed();
        }
    }
}
