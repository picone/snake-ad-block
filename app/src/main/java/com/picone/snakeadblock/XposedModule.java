package com.picone.snakeadblock;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.ViewGroup;

import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Random;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class XposedModule implements IXposedHookLoadPackage {

    private static final String LOG_TAG = "SnakeAdBlock";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!Constants.SNAKE_PACKAGE_NAME.equals(lpparam.packageName)) {
            return;
        }
        hideLittleGameAd(lpparam); // 小游戏，如转盘等广告
        hideSplashAd(lpparam); // 开屏广告
        hideVideoRewardAd(lpparam); // 奖励广告
        modifySingleGameScore(lpparam); // 修改单机游戏的分数
    }

    private void hideSplashAd(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            findAndHookMethod("a.a.a.a.a.c.f.a", lpparam.classLoader, "a", ViewGroup.class, String.class, "com.miui.zeus.mimo.sdk.SplashAd.SplashAdListener", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    return null;
                }
            });
        } catch (XposedHelpers.ClassNotFoundError | NoSuchMethodError e) {
            Log.w(LOG_TAG, "can not find method:" + e);
        } catch (Exception e) {
            Log.w(LOG_TAG, e);
        }
    }

    private void hideLittleGameAd(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> cocosReqProcessorClazz = findClass("com.wepie.snake.cocos.CocosReqProcessor", lpparam.classLoader);
            Method publishResultMethod = cocosReqProcessorClazz.getMethod("publishResult", boolean.class, String.class, int.class);
            findAndHookMethod(cocosReqProcessorClazz, "showAd", Activity.class, String.class, String.class, new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        Log.d(LOG_TAG, "on cocos req ad,arg1:" + param.args[1] + ",arg2:" + param.args[2]);
                        JSONObject data = new JSONObject((String) param.args[2]);
                        int seq = data.getInt("seq");
                        publishResultMethod.invoke(null, true, "show_ad", seq);
                    } catch (Exception e) {
                        Log.w(LOG_TAG, e);
                    }
                    return null;
                }
            });
            findAndHookMethod(cocosReqProcessorClazz, "checkAd", Activity.class, String.class, String.class, new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    Log.d(LOG_TAG, "on cocos check ad,arg1:" + param.args[1] + ",arg2:" + param.args[2]);
                    return true;
                }
            });
        } catch (XposedHelpers.ClassNotFoundError | NoSuchMethodError e) {
            Log.w(LOG_TAG, "can not find method:" + e);
        } catch (Exception e) {
            Log.w(LOG_TAG, e);
        }
    }

    private void hideVideoRewardAd(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            findAndHookMethod("com.wepie.snake.module.a.c", lpparam.classLoader, "a", Activity.class, int.class, int.class, "com.wepie.ad.base.c", boolean.class, HashMap.class, new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        Object listener = param.args[3];
                        Log.d(LOG_TAG, "AdHelper show ad, listener:" + listener);
                        listener.getClass().getDeclaredMethod("onSuccess", String.class).
                                invoke(listener, "mimo_video");
                    } catch (Exception e) {
                        Log.w(LOG_TAG, e);
                    }
                    return null;
                }
            });
        } catch (XposedHelpers.ClassNotFoundError | NoSuchMethodError e) {
            Log.w(LOG_TAG, "can not find method:" + e);
        } catch (Exception e) {
            Log.w(LOG_TAG, e);
        }
    }

    private void modifySingleGameScore(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> modelClazz = findClass("com.wepie.snake.module.game.logic.a.b", lpparam.classLoader);
            Field lengthField = modelClazz.getDeclaredField("a");
            Field killField = modelClazz.getDeclaredField("b");
            Field gameTimeField = modelClazz.getDeclaredField("e");
            Field destroyLengthField = modelClazz.getDeclaredField("g");
            Field collectField = modelClazz.getDeclaredField("j");
            findAndHookMethod("com.wepie.snake.module.game.logic.b", lpparam.classLoader, "a", Context.class, "com.wepie.snake.module.game.logic.a.b", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    Random rnd = new Random();
                    lengthField.setInt(param.args[1], 100000 + rnd.nextInt(3000000));
                    gameTimeField.setInt(param.args[1], 300 + rnd.nextInt(100));
                    killField.setInt(param.args[1], 10 + rnd.nextInt(100));
                    destroyLengthField.setInt(param.args[1], 13000 + rnd.nextInt(100000));
                    collectField.setInt(param.args[1], 260 + rnd.nextInt(10));
                }
            });
        } catch (XposedHelpers.ClassNotFoundError | NoSuchMethodError e) {
            Log.w(LOG_TAG, "can not find method:" + e);
        } catch (Exception e) {
            Log.w(LOG_TAG, e);
        }
    }
}
