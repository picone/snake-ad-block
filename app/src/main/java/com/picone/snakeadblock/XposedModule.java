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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
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
            findAndHookMethod("c.a.a.a.a.c.f.a", lpparam.classLoader, "a", ViewGroup.class, String.class, "com.miui.zeus.mimo.sdk.SplashAd.SplashAdListener", new XC_MethodReplacement() {
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
            findAndHookMethod("com.wepie.snake.module.advertisement.b", lpparam.classLoader, "a", Activity.class, int.class, int.class, "com.wepie.ad.base.c", boolean.class, HashMap.class, new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) {
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
            Class<?> gameEndEntityClazz = findClass("com.wepie.snake.module.game.logic.a.d", lpparam.classLoader);
            Field lengthField = gameEndEntityClazz.getDeclaredField("a");
            Field killField = gameEndEntityClazz.getDeclaredField("b");
            Field gameTimeField = gameEndEntityClazz.getDeclaredField("e");
            Field destroyLengthField = gameEndEntityClazz.getDeclaredField("g");
            Field collectField = gameEndEntityClazz.getDeclaredField("j");
            Field activityField = gameEndEntityClazz.getDeclaredField("u");

            Class<?> gameActivityEntityClazz = findClass("com.wepie.snake.module.game.logic.a.b", lpparam.classLoader);
            Field activityIdField  = gameActivityEntityClazz.getDeclaredField("a");
            Field collectListField  = gameActivityEntityClazz.getDeclaredField("b");

            Class<?> gameActivityItemClazz = findClass("com.wepie.snake.module.game.logic.a.b.a", lpparam.classLoader);
            Field activityItemId = gameActivityItemClazz.getDeclaredField("a");
            Field activityItemNum = gameActivityItemClazz.getDeclaredField("b");

            findAndHookMethod("com.wepie.snake.module.game.logic.b", lpparam.classLoader, "a", Context.class, gameEndEntityClazz, "com.wepie.snake.net.http.b.m$a", new XC_MethodHook() {
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
                    // activity
                    Object activity = activityField.get(endGameParam);
                    if (activity != null) {
                        Object activityId = activityIdField.get(activity);
                        if (Objects.equals((String) activityId, "101")) {
                            // 美食节活动，创建新的随机收集到的列表
                            // 普通：12-25
                            // 特别：101-104
                            List<Object> newCollectList = new ArrayList<>();
                            for (int i = 12; i <= 25; i++) {
                                Object item = gameActivityItemClazz.newInstance();
                                activityItemId.setInt(item, i);
                                activityItemNum.setInt(item, 1+rnd.nextInt(30));
                                newCollectList.add(item);
                            }
                            Object item = gameActivityItemClazz.newInstance();
                            activityItemId.setInt(item, 101);
                            activityItemNum.setInt(item, 1+rnd.nextInt(100));
                            newCollectList.add(item);
                            item = gameActivityItemClazz.newInstance();
                            activityItemId.setInt(item, 102);
                            activityItemNum.setInt(item, 1+rnd.nextInt(30));
                            newCollectList.add(item);
                            item = gameActivityItemClazz.newInstance();
                            activityItemId.setInt(item, 103);
                            activityItemNum.setInt(item, 1+rnd.nextInt(10));
                            newCollectList.add(item);
                            item = gameActivityItemClazz.newInstance();
                            activityItemId.setInt(item, 104);
                            activityItemNum.setInt(item, 1+rnd.nextInt(10));
                            newCollectList.add(item);
                            collectListField.set(activity, newCollectList);
                            Log.w(LOG_TAG, "use new list, len=" + newCollectList.size());
                        }
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
