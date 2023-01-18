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
            findAndHookMethod("com.wepie.snake.module.advertisement.ADHelper", lpparam.classLoader, "showVideo", Activity.class, int.class, int.class, "com.wepie.ad.base.IAdShowCallback", boolean.class, HashMap.class, new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        Object callback = param.args[3];
                        callback.getClass().getDeclaredMethod("onSuccess", String.class).
                                invoke(callback, "mimo_video");
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
            Class<?> modelClazz = findClass("com.wepie.snake.module.game.logic.entity.GameEndParam", lpparam.classLoader);
            Field lengthField = modelClazz.getDeclaredField("length");
            Field killField = modelClazz.getDeclaredField("kill");
            Field gameTimeField = modelClazz.getDeclaredField("survivalTime");
            Field destroyLengthField = modelClazz.getDeclaredField("destroyLength");
            Field collectField = modelClazz.getDeclaredField("floaterCount");
            findAndHookMethod("com.wepie.snake.module.game.logic.OffGameOverManager", lpparam.classLoader, "requestGameOverData", Context.class, modelClazz, "com.wepie.snake.net.http.handler.SendScoreHandler.Callback", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    Object endGameParam = param.args[1];
                    Random rnd = new Random();
                    lengthField.setInt(endGameParam, 100000 + rnd.nextInt(3000000));
                    gameTimeField.setInt(endGameParam, 300 + rnd.nextInt(100));
                    killField.setInt(endGameParam, 10 + rnd.nextInt(100));
                    destroyLengthField.setInt(endGameParam, 13000 + rnd.nextInt(100000));
                    if (collectField.getInt(endGameParam) > 0) {
                        collectField.setInt(endGameParam, 260 + rnd.nextInt(10));
                    }
                }
            });
        } catch (XposedHelpers.ClassNotFoundError | NoSuchMethodError e) {
            Log.w(LOG_TAG, "can not find method:" + e);
        } catch (Exception e) {
            Log.w(LOG_TAG, e);
        }
    }
}
