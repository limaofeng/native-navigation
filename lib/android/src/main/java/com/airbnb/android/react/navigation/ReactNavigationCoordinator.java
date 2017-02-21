package com.airbnb.android.react.navigation;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import com.facebook.react.ReactInstanceManager;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableMap;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReactNavigationCoordinator {
  public static ReactNavigationCoordinator sharedInstance = new ReactNavigationCoordinator();

  private ReactInstanceManager reactInstanceManager;
  private NavigationImplementation navigationImplementation = new DefaultNavigationImplementation();
  private boolean isSuccessfullyInitialized = false;

  private ReactNavigationCoordinator() {
  }

  public ReactInstanceManager getReactInstanceManager() {
    return reactInstanceManager;
  }

  public void injectReactInstanceManager(final ReactInstanceManager reactInstanceManager) {
    if (this.reactInstanceManager != null) {
      // TODO: throw error. can only initialize once.
    }
    this.reactInstanceManager = reactInstanceManager;
    this.reactInstanceManager.addReactInstanceEventListener(
      new ReactInstanceManager.ReactInstanceEventListener() {
        @Override
        public void onReactContextInitialized(ReactContext context) {
          reactInstanceManager.removeReactInstanceEventListener(this);
          isSuccessfullyInitialized = true;
        }
      });
  }
  public void injectImplementation(NavigationImplementation implementation) {
    if (this.navigationImplementation != null) {
      // TODO: throw error. can only initialize once.
    }
    this.navigationImplementation = implementation;
  }

  public NavigationImplementation getImplementation() {
    return this.navigationImplementation;
  }

  boolean isSuccessfullyInitialized() {
    return isSuccessfullyInitialized;
  }

  public void injectExposedActivities(List<ReactExposedActivityParams> exposedActivities) {
    // TODO(lmr): would it make sense to warn or throw here if it's already set?
    this.exposedActivities = exposedActivities;
  }

  /**
   * NOTE(lmr): In the future, we would like to replace this with an annotation parser that
   * generates this map based off of the `ReactExposedActivity` annotations. For now, this should
   * work well enough in the interim.
   */
  private List<ReactExposedActivityParams> exposedActivities;
  private final Map<String /* instance id */, WeakReference<ReactInterface>> componentsMap = new HashMap<>();
  private final Map<String /* instance id */, Boolean> dismissCloseBehaviorMap = new HashMap<>();
  private final Map<String /* name */, ReadableMap> screenInitialConfigMap = new HashMap<>();

  public void registerComponent(ReactInterface component, String name) {
    componentsMap.put(name, new WeakReference<>(component));
  }

  public void unregisterComponent(String name) {
    componentsMap.remove(name);
  }

  /**
   * Returns an {@link Intent} used for launching an {@link Activity} exposed to React Native flows
   * based on the provided {@code key}. Will pass the provided {@code arguments} as {@link Intent}
   * extras. Activities should have been previously registered via {@code exposedActivities} in the
   * {@link ReactNavigationCoordinator} constructor.
   *
   * @see ReactExposedActivityParams#toIntent(Context, ReadableMap)
   */
  Intent intentForKey(Context context, String key, ReadableMap arguments) {
    for (ReactExposedActivityParams exposedActivity : exposedActivities) {
      if (exposedActivity.key().equals(key)) {
        return exposedActivity.toIntent(context, arguments);
      }
    }
    throw new IllegalArgumentException(
        String.format("Tried to push Activity with key '%s', but it could not be found", key));
  }

  ReactAwareActivityFacade activityFromId(String id) {
    WeakReference<ReactInterface> ref = componentsMap.get(id);
    return ref == null ? null : (ReactAwareActivityFacade) ref.get().getActivity();
  }

  ReactInterface componentFromId(String id) {
    WeakReference<ReactInterface> ref = componentsMap.get(id);
    return ref == null ? null : ref.get();
  }

  // If set to true, the Activity will be dismissed when its Toolbar NavigationIcon (home button) is clicked,
  // instead of performing the default behavior (finish)
  public void setDismissCloseBehavior(String id, boolean dismissClose) {
    dismissCloseBehaviorMap.put(id, dismissClose);
  }

  public boolean getDismissCloseBehavior(ReactInterface reactInterface) {
    String id = reactInterface.getInstanceId();
    Boolean dismissClose = dismissCloseBehaviorMap.get(id);
    return dismissClose != null && dismissClose;
  }

  public void setInitialConfigForModuleName(String sceneName, ReadableMap config) {
    screenInitialConfigMap.put(sceneName, config);
  }

  public ReadableMap getInitialConfigForModuleName(String sceneName) {
    if (screenInitialConfigMap.containsKey(sceneName)) {
      return screenInitialConfigMap.get(sceneName);
    } else {
      return ConversionUtil.EMPTY_MAP;
    }
  }
}