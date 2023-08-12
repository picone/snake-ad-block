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
                        if (Objects.equals(activityId, "101")) {
                            // 美食节活动，创建新的随机收集到的列表
                            // 普通：12-25
                            // 特别：101-104
                            List<Object> newCollectList = new ArrayList<>();
                            for (int i = 1; i <= 3; i++) {
                                Object item = gameActivityItemClazz.newInstance();
                                activityItemId.setInt(item, i);
                                activityItemNum.setInt(item, 1+rnd.nextInt(30));
                                newCollectList.add(item);
                            }
                            for (int i = 7; i <= 11; i++) {
                                Object item = gameActivityItemClazz.newInstance();
                                activityItemId.setInt(item, i);
                                activityItemNum.setInt(item, 1+rnd.nextInt(30));
                                newCollectList.add(item);
                            }
                            for (int i = 31; i <= 34; i++) {
                                Object item = gameActivityItemClazz.newInstance();
                                activityItemId.setInt(item, i);
                                activityItemNum.setInt(item, 1+rnd.nextInt(30));
                                newCollectList.add(item);
                            }
                            Object item = gameActivityItemClazz.newInstance();
                            activityItemId.setInt(item, 21);
                            activityItemNum.setInt(item, 1+rnd.nextInt(20));
                            newCollectList.add(item);
                            item = gameActivityItemClazz.newInstance();
                            activityItemId.setInt(item, 105);
                            activityItemNum.setInt(item, 1+rnd.nextInt(20));
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

/**
 * {
 * 	"code": 200,
 * 	"data": {
 * 		"desc": "https://sca.tcsdzz.com/snake_file_1682255399_tscg.png",
 * 		"floating": [{
 * 			"desc": "故人想吃狮子头，烟花三月下扬州。",
 * 			"floating_name": "狮子头",
 * 			"id": 12,
 * 			"imgurl": "https://sca.tcsdzz.com/snake_file_1681715196_tiuh.png",
 * 			"is_open": 1,
 * 			"is_rare": 0,
 * 			"name": "江苏",
 * 			"need_num": 80,
 * 			"progress": 80,
 * 			"reward": [{
 * 				"type": 7,
 * 				"skin_id": 20012,
 * 				"num": 5,
 * 				"name": "复活卡",
 * 				"imgurl": "https://sca.tcsdzz.com/snake_file_1530588386561956.png"
 *                        }],
 * 			"show_state": 0,
 * 			"start_desc": "",
 * 			"state": 3* 		}, {
 * 			"desc": "我最喜欢的东坡肉，一人一块，你可以吃两块。",
 * 			"floating_name": "东坡肉",
 * 			"id": 13,
 * 			"imgurl": "https://sca.tcsdzz.com/snake_file_1681715224_tnja.png",
 * 			"is_open": 1,
 * 			"is_rare": 1,
 * 			"name": "浙江",
 * 			"need_num": 50,
 * 			"progress": 50,
 * 			"reward": [{
 * 				"type": 9,
 * 				"skin_id": 30120,
 * 				"num": 3,
 * 				"name": "深海珍珠",
 * 				"imgurl": "https://sca.tcsdzz.com/snake_gift_1632377817.png"* 			}],
 * 			"show_state": 0,
 * 			"start_desc": "",
 * 			"state        3
 * 		}, {
 * 			"desc": "桃花流水鳜鱼肥。",
 * 			"floating_name": "臭鳜鱼",
 * 			"id": 14,
 * 			"imgurl": "https://sca.tcsdzz.com/snake_file_1681715228_tilu.png",
 * 			"is_open": 1,
 * 			"is_rare": 1,
 * 			"name": "安徽",
 * 			"need_num": 50,
 * 			"progress": 50,
 * 			"reward": [{
 * 				"type": 7,
 * 				"skin_id": 20006,
 * 				"num": 1,
 * 				"name": "6元红包",
 * 				"imgurl": "https://sca.tcsdzz.com/snake_file_1530588386457578.png"
 * 			}],
 * 			"show_state": 0,
 * 			"start_desc": "",
 * 			"state        3
 * 		}, {
 * 			"desc": "土笋冻里没有笋哦～",
 * 			"floating_name": "土笋冻",
 * 			"id": 15,
 * 			"imgurl": "https://sca.tcsdzz.com/snake_file_1681715375_tmhs.png",
 * 			"is_open": 1,
 * 			"is_rare": 0,
 * 			"name": "福建",
 * 			"need_num": 30,
 * 			"progress": 30,
 * 			"reward": [{
 * 				"type": 5,
 * 				"skin_id": 11247,
 * 				"num": 1,
 * 				"name": "土笋冻",
 * 				"imgurl": "https://sca.tcsdzz.com/snake_file_1682304281_tomr.png"
 * 			}],
 * 			"show_state": 0,
 * 			"start_desc": "",
 * 			"state        3
 * 		}, {
 * 			"desc": "小火慢煨，滋味才对～",
 * 			"floating_name": "瓦罐汤",
 * 			"id": 16,
 * 			"imgurl": "https://sca.tcsdzz.com/snake_file_1681715407_tfbf.png",
 * 			"is_open": 1,
 * 			"is_rare": 0,
 * 			"name": "江西",
 * 			"need_num": 100,
 * 			"progress": 100,
 * 			"reward": [{
 * 				"type": 33,
 * 				"skin_id": 0,
 * 				"num": 200,
 * 				"name": "好运金券",
 * 				"imgurl": "https://sca.tcsdzz.com/race/tbid1670308984587.png"
 * 			}],
 * 			"show_state": 0,
 * 			"start_desc": "",
 * 			"state        3
 * 		}, {
 * 			"desc": "一把子喜欢了！",
 * 			"floating_name": "把子肉",
 * 			"id": 17,
 * 			"imgurl": "https://sca.tcsdzz.com/snake_file_1681715412_tpfr.png",
 * 			"is_open": 1,
 * 			"is_rare": 0,
 * 			"name": "山东",
 * 			"need_num": 80,
 * 			"progress": 80,
 * 			"reward": [{
 * 				"type": 9,
 * 				"skin_id": 30127,
 * 				"num": 10,
 * 				"name": "爱的便当",
 * 				"imgurl": "https://sca.tcsdzz.com/snake_gift_1637033377.png"
 * 			}],
 * 			"show_state": 0,
 * 			"start_desc": "",
 * 			"state        3
 * 		}, {
 * 			"desc": "热乎爆汁的生煎，是清晨的幸福滋味。",
 * 			"floating_name": "生煎包",
 * 			"id": 18,
 * 			"imgurl": "https://sca.tcsdzz.com/snake_file_1681715416_tbak.png",
 * 			"is_open": 1,
 * 			"is_rare": 0,
 * 			"name": "上海",
 * 			"need_num": 100,
 * 			"progress": 100,
 * 			"reward": [{
 * 				"type": 9,
 * 				"skin_id": 30138,
 * 				"num": 10,
 * 				"name": "樱花甜点",
 * 				"imgurl": "https://sca.tcsdzz.com/snake_gift_1648470168.png"
 * 			}],
 * 			"show_state": 0,
 * 			"start_desc": "",
 * 			"state        3
 * 		}, {
 * 			"desc": "早茶千千万，虾饺不能断！",
 * 			"floating_name": "虾饺",
 * 			"id": 19,
 * 			"imgurl": "https://sca.tcsdzz.com/snake_file_1681715471_tudl.png",
 * 			"is_open": 1,
 * 			"is_rare": 0,
 * 			"name": "广东",
 * 			"need_num": 80,
 * 			"progress": 80,
 * 			"reward": [{
 * 				"type": 9,
 * 				"skin_id": 30158,
 * 				"num": 10,
 * 				"name": "绵软下午茶",
 * 				"imgurl": "https://sca.tcsdzz.com/snake_gift_1587611337.png"
 * 			}],
 * 			"show_state": 0,
 * 			"start_desc": "",
 * 			"state        3
 * 		}, {
 * 			"desc": "耶！吃鸡！",
 * 			"floating_name": "椰子鸡",
 * 			"id": 20,
 * 			"imgurl": "https://sca.tcsdzz.com/snake_file_1681715474_tmfk.png",
 * 			"is_open": 1,
 * 			"is_rare": 1,
 * 			"name": "海南",
 * 			"need_num": 50,
 * 			"progress": 50,
 * 			"reward": [{
 * 				"type": 9,
 * 				"skin_id": 30120,
 * 				"num": 3,
 * 				"name": "深海珍珠",
 * 				"imgurl": "https://sca.tcsdzz.com/snake_gift_1632377817.png"
 * 			}],
 * 			"show_state": 0,
 * 			"start_desc": "",
 * 			"state        3
 * 		}, {
 * 			"desc": "来碗螺蛳粉，加个炸蛋，酸笋多多！",
 * 			"floating_name": "螺蛳粉",
 * 			"id": 22,
 * 			"imgurl": "https://sca.tcsdzz.com/snake_file_1681715515_tvtg.png",
 * 			"is_open": 1,
 * 			"is_rare": 1,
 * 			"name": "广西",
 * 			"need_num": 20,
 * 			"progress": 20,
 * 			"reward": [{
 * 				"type": 5,
 * 				"skin_id": 11245,
 * 				"num": 1,
 * 				"name": "螺蛳粉",
 * 				"imgurl": "https://sca.tcsdzz.com/snake_file_1682304035_ttts.png"
 * 			}],
 * 			"show_state": 0,
 * 			"start_desc": "",
 * 			"state        3
 * 		}, {
 * 			"desc": "你要来一碗热腾腾的胡辣汤吗？",
 * 			"floating_name": "胡辣汤",
 * 			"id": 23,
 * 			"imgurl": "https://sca.tcsdzz.com/snake_file_1681715518_tcth.png",
 * 			"is_open": 1,
 * 			"is_rare": 0,
 * 			"name": "河南",
 * 			"need_num": 150,
 * 			"progress": 150,
 * 			"reward": [{
 * 				"type": 9,
 * 				"skin_id": 30138,
 * 				"num": 10,
 * 				"name": "樱花甜点",
 * 				"imgurl": "https://sca.tcsdzz.com/snake_gift_1648470168.png"
 * 			}],
 * 			"show_state": 0,
 * 			"start_desc": "",
 * 			"state        3
 * 		}, {
 * 			"desc": "我是你的早餐首选吗？",
 * 			"floating_name": "热干面",
 * 			"id": 24,
 * 			"imgurl": "https://sca.tcsdzz.com/snake_file_1681715522_thtj.png",
 * 			"is_open": 1,
 * 			"is_rare": 0,
 * 			"name": "湖北",
 * 			"need_num": 200,
 * 			"progress": 200,
 * 			"reward": [{
 * 				"type": 9,
 * 				"skin_id": 30138,
 * 				"num": 10,
 * 				"name": "樱花甜点",
 * 				"imgurl": "https://sca.tcsdzz.com/snake_gift_1648470168.png"
 * 			}],
 * 			"show_state": 0,
 * 			"start_desc": "",
 * 			"state        3
 * 		}, {
 * 			"desc": "闻着臭，吃着香",
 * 			"floating_name": "臭豆腐",
 * 			"id": 25,
 * 			"imgurl": "https://sca.tcsdzz.com/snake_file_1681715548_ttaz.png",
 * 			"is_open": 1,
 * 			"is_rare": 1,
 * 			"name": "湖南",
 * 			"need_num": 10,
 * 			"progress": 10,
 * 			"reward": [{
 * 				"type": 5,
 * 				"skin_id": 11243,
 * 				"num": 1,
 * 				"name": "臭豆腐",
 * 				"imgurl": "https://sca.tcsdzz.com/snake_file_1682303654_tfew.png"
 * 			}],
 * 			"show_state": 0,
 * 			"start_desc": "",
 * 			"state        3
 * 		}, {
 * 			"desc": "5.13后，玩无尽游戏概率遇到",
 * 			"floating_name": "锅包肉",
 * 			"id": 1,
 * 			"imgurl": "https://sca.tcsdzz.com/snake_file_1681714895_tthd.png",
 * 			"is_open": 1,
 * 			"is_rare": 0,
 * 			"name": "黑龙江",
 * 			"need_num": 100,
 * 			"progress": 0,
 * 			"reward": [{
 * 				"type": 7,
 * 				"skin_id": 20012,
 * 				"num": 5,
 * 				"name": "复活卡",
 * 				"imgurl": "https://sca.tcsdzz.com/snake_file_1530588386561956.png"
 * 			}],
 * 			"show_state": 0,
 * 			"start_desc": "",
 * 			"state        1
 * 		}, {
 * 			"desc": "5.13后，无尽游戏本局长度达到50000分后概率遇到",
 * 			"floating_name": "雪衣豆沙",
 * 			"id": 2,
 * 			"imgurl": "https://sca.tcsdzz.com/snake_file_1681714917_tidw.png",
 * 			"is_open": 1,
 * 			"is_rare": 1,
 * 			"name": "吉林",
 * 			"need_num": 60,
 * 			"progress": 0,
 * 			"reward": [{
 * 				"type": 7,
 * 				"skin_id": 20057,
 * 				"num": 1,
 * 				"name": "限时返利券（1日）",
 * 				"imgurl": "https://sca.tcsdzz.com/snake_prop_1568261288.png"
 * 			}],
 * 			"show_state": 0,
 * 			"start_desc": "",
 * 			"state        1
 * 		}, {
 * 			"desc": "5.13后，玩无尽游戏概率遇到",
 * 			"floating_name": "猪肉炖粉条",
 * 			"id": 3,
 * 			"imgurl": "https://sca.tcsdzz.com/snake_file_1681714928_thgd.png",
 * 			"is_open": 1,
 * 			"is_rare": 0,
 * 			"name": "辽宁",
 * 			"need_num": 80,
 * 			"progress": 0,
 * 			"reward": [{
 * 				"type": 33,
 * 				"skin_id": 0,
 * 				"num": 200,
 * 				"name": "好运金券",
 * 				"imgurl": "https://sca.tcsdzz.com/race/tbid1670308984587.png"
 * 			}],
 * 			"show_state": 0,
 * 			"start_desc": "",
 * 			"state        1
 * 		}, {
 * 			"desc": "5.13后，玩无尽游戏概率遇到",
 * 			"floating_name": "驴肉火烧",
 * 			"id": 7,
 * 			"imgurl": "https://sca.tcsdzz.com/snake_file_1681714975_tgmx.png",
 * 			"is_open": 1,
 * 			"is_rare": 0,
 * 			"name": "河北",
 * 			"need_num": 80,
 * 			"progress": 0,
 * 			"reward": [{
 * 				"type": 9,
 * 				"skin_id": 30138,
 * 				"num": 10,
 * 				"name": "樱花甜点",
 * 				"imgurl": "https://sca.tcsdzz.com/snake_gift_1648470168.png"
 * 			}],
 * 			"show_state": 0,
 * 			"start_desc": "",
 * 			"state        1
 * 		}, {
 * 			"desc": "5.13后，点亮5个省份，玩无尽游戏概率遇到",
 * 			"floating_name": "刀削面",
 * 			"id": 8,
 * 			"imgurl": "https://sca.tcsdzz.com/snake_file_1681714981_taic.png",
 * 			"is_open": 1,
 * 			"is_rare": 0,
 * 			"name": "山西",
 * 			"need_num": 80,
 * 			"progress": 0,
 * 			"reward": [{
 * 				"type": 9,
 * 				"skin_id": 30127,
 * 				"num": 10,
 * 				"name": "爱的便当",
 * 				"imgurl": "https://sca.tcsdzz.com/snake_gift_1637033377.png"
 * 			}],
 * 			"show_state": 0,
 * 			"start_desc": "",
 * 			"state        1
 * 		}, {
 * 			"desc": "5.13后，无尽游戏本局长度达到100000分后概率遇到",
 * 			"floating_name": "烤全羊",
 * 			"id": 9,
 * 			"imgurl": "https://sca.tcsdzz.com/snake_file_1681715156_tmya.png",
 * 			"is_open": 1,
 * 			"is_rare": 0,
 * 			"name": "内蒙古",
 * 			"need_num": 80,
 * 			"progress": 0,
 * 			"reward": [{
 * 				"type": 7,
 * 				"skin_id": 20006,
 * 				"num": 1,
 * 				"name": "6元红包",
 * 				"imgurl": "https://sca.tcsdzz.com/snake_file_1530588386457578.png"
 * 			}],
 * 			"show_state": 0,
 * 			"start_desc": "",
 * 			"state        1
 * 		}, {
 * 			"desc": "5.13后，无尽游戏本局长度达到300000分后概率遇到",
 * 			"floating_name": "佛跳墙",
 * 			"id": 10,
 * 			"imgurl": "https://sca.tcsdzz.com/snake_file_1681715189_tfgw.png",
 * 			"is_open": 1,
 * 			"is_rare": 1,
 * 			"name": "北京",
 * 			"need_num": 30,
 * 			"progress": 0,
 * 			"reward": [{
 * 				"type": 9,
 * 				"skin_id": 30120,
 * 				"num": 3,
 * 				"name": "深海珍珠",
 * 				"imgurl": "https://sca.tcsdzz.com/snake_gift_1632377817.png"
 * 			}],
 * 			"show_state": 0,
 * 			"start_desc": "",
 * 			"state        1
 * 		}, {
 * 			"desc": "5.13后，玩无尽游戏概率遇到",
 * 			"floating_name": "狗不理包子",
 * 			"id": 11,
 * 			"imgurl": "https://sca.tcsdzz.com/snake_file_1681715192_tbsp.png",
 * 			"is_open": 1,
 * 			"is_rare": 0,
 * 			"name": "天津",
 * 			"need_num": 100,
 * 			"progress": 0,
 * 			"reward": [{
 * 				"type": 9,
 * 				"skin_id": 30127,
 * 				"num": 10,
 * 				"name": "爱的便当",
 * 				"imgurl": "https://sca.tcsdzz.com/snake_gift_1637033377.png"
 * 			}],
 * 			"show_state": 0,
 * 			"start_desc": "",
 * 			"state        1
 * 		}, {
 * 			"desc": "5.13后，玩无尽游戏概率遇到",
 * 			"floating_name": "折耳根",
 * 			"id": 31,
 * 			"imgurl": "https://sca.tcsdzz.com/snake_file_1681725459_tyrs.png",
 * 			"is_open": 1,
 * 			"is_rare": 0,
 * 			"name": "贵州",
 * 			"need_num": 60,
 * 			"progress": 0,
 * 			"reward": [{
 * 				"type": 9,
 * 				"skin_id": 30138,
 * 				"num": 10,
 * 				"name": "樱花甜点",
 * 				"imgurl": "https://sca.tcsdzz.com/snake_gift_1648470168.png"
 * 			}],
 * 			"show_state": 0,
 * 			"start_desc": "",
 * 			"state        1
 * 		}, {
 * 			"desc": "5.13后，玩无尽游戏概率遇到",
 * 			"floating_name": "过桥米线",
 * 			"id": 32,
 * 			"imgurl": "https://sca.tcsdzz.com/snake_file_1681715646_thpg.png",
 * 			"is_open": 1,
 * 			"is_rare": 0,
 * 			"name": "云南",
 * 			"need_num": 60,
 * 			"progress": 0,
 * 			"reward": [{
 * 				"type": 33,
 * 				"skin_id": 0,
 * 				"num": 200,
 * 				"name": "好运金券",
 * 				"imgurl": "https://sca.tcsdzz.com/race/tbid1670308984587.png"
 * 			}],
 * 			"show_state": 0,
 * 			"start_desc": "",
 * 			"state        1
 * 		}, {
 * 			"desc": "5.13后，无尽游戏本局长度达到10000分后概率遇到",
 * 			"floating_name": "牦牛肉干",
 * 			"id": 33,
 * 			"imgurl": "https://sca.tcsdzz.com/snake_file_1681994176_tjdh.png",
 * 			"is_open": 1,
 * 			"is_rare": 1,
 * 			"name": "西藏",
 * 			"need_num": 60,
 * 			"progress": 0,
 * 			"reward": [{
 * 				"type": 9,
 * 				"skin_id": 30158,
 * 				"num": 10,
 * 				"name": "绵软下午茶",
 * 				"imgurl": "https://sca.tcsdzz.com/snake_gift_1587611337.png"
 * 			}],
 * 			"show_state": 0,
 * 			"start_desc": "",
 * 			"state        1
 * 		}, {
 * 			"desc": "5.13后，点亮10个省份，玩无尽游戏概率遇到",
 * 			"floating_name": "火锅",
 * 			"id": 34,
 * 			"imgurl": "https://sca.tcsdzz.com/snake_file_1681725487_tnme.png",
 * 			"is_open": 1,
 * 			"is_rare": 0,
 * 			"name": "重庆",
 * 			"need_num": 80,
 * 			"progress": 0,
 * 			"reward": [{
 * 				"type": 9,
 * 				"skin_id": 30158,
 * 				"num": 10,
 * 				"name": "绵软下午茶",
 * 				"imgurl": "https://sca.tcsdzz.com/snake_gift_1587611337.png"
 * 			}],
 * 			"show_state": 0,
 * 			"start_desc": "",
 * 			"state        1
 * 		}, {
 * 			"desc": "我是一条酸菜鱼，又酸又菜又多余😭",
 * 			"floating_name": "水煮鱼",
 * 			"id": 21,
 * 			"imgurl": "https://sca.tcsdzz.com/snake_file_1681715480_txwv.png",
 * 			"is_open": 1,
 * 			"is_rare": 0,
 * 			"name": "四川",
 * 			"need_num": 100,
 * 			"progress": 37,
 * 			"reward": [{
 * 				"type": 9,
 * 				"skin_id": 30138,
 * 				"num": 10,
 * 				"name": "樱花甜点",
 * 				"imgurl": "https://sca.tcsdzz.com/snake_gift_1648470168.png"
 * 			}],
 * 			"show_state": 0,
 * 			"start_desc": "",
 * 			"state        1
 * 		}, {
 * 			"desc": "5.28后，无尽游戏本局长度达到10000分后概率遇到",
 * 			"floating_name": "凉皮",
 * 			"id": 26,
 * 			"imgurl": "https://sca.tcsdzz.com/snake_file_1681715553_tedy.png",
 * 			"is_open": 0,
 * 			"is_rare": 0,
 * 			"name": "陕西",
 * 			"need_num": 100,
 * 			"progress": 0,
 * 			"reward": [{
 * 				"type": 7,
 * 				"skin_id": 20012,
 * 				"num": 5,
 * 				"name": "复活卡",
 * 				"imgurl": "https://sca.tcsdzz.com/snake_file_1530588386561956.png"
 * 			}],
 * 			"show_state": 0,
 * 			"start_desc": "05.28",
 * 			"state        1
 * 		}, {
 * 			"desc": "5.28后，无尽游戏本局长度达到20000分后概率遇到",
 * 			"floating_name": "牛肉面",
 * 			"id": 27,
 * 			"imgurl": "https://sca.tcsdzz.com/snake_file_1681715556_tmun.png",
 * 			"is_open": 0,
 * 			"is_rare": 0,
 * 			"name": "甘肃",
 * 			"need_num": 88,
 * 			"progress": 0,
 * 			"reward": [{
 * 				"type": 33,
 * 				"skin_id": 0,
 * 				"num": 200,
 * 				"name": "好运金券",
 * 				"imgurl": "https://sca.tcsdzz.com/race/tbid1670308984587.png"
 * 			}],
 * 			"show_state": 0,
 * 			"start_desc": "05.28",
 * 			"state        1
 * 		}, {
 * 			"desc": "5.28后，无尽游戏本局长度达到150000分后概率遇到",
 * 			"floating_name": "青海土火锅",
 * 			"id": 28,
 * 			"imgurl": "https://sca.tcsdzz.com/snake_file_1681715590_tdzu.png",
 * 			"is_open": 0,
 * 			"is_rare": 1,
 * 			"name": "青海",
 * 			"need_num": 50,
 * 			"progress": 0,
 * 			"reward": [{
 * 				"type": 9,
 * 				"skin_id": 30158,
 * 				"num": 10,
 * 				"name": "绵软下午茶",
 * 				"imgurl": "https://sca.tcsdzz.com/snake_gift_1587611337.png"
 * 			}],
 * 			"show_state": 0,
 * 			"start_desc": "05.28",
 * 			"state        1
 * 		}, {
 * 			"desc": "5.28后，玩无尽游戏概率遇到",
 * 			"floating_name": "手抓羊排",
 * 			"id": 29,
 * 			"imgurl": "https://sca.tcsdzz.com/snake_file_1681715596_tila.png",
 * 			"is_open": 0,
 * 			"is_rare": 0,
 * 			"name": "宁夏",
 * 			"need_num": 100,
 * 			"progress": 0,
 * 			"reward": [{
 * 				"type": 7,
 * 				"skin_id": 20006,
 * 				"num": 1,
 * 				"name": "6元红包",
 * 				"imgurl": "https://sca.tcsdzz.com/snake_file_1530588386457578.png"
 * 			}],
 * 			"show_state": 0,
 * 			"start_desc": "05.28",
 * 			"state        1
 * 		}, {
 * 			"desc": "5.28后，玩无尽游戏概率遇到",
 * 			"floating_name": "烤羊肉串",
 * 			"id": 30,
 * 			"imgurl": "https://sca.tcsdzz.com/snake_file_1681715618_twsc.png",
 * 			"is_open": 0,
 * 			"is_rare": 0,
 * 			"name": "新疆",
 * 			"need_num": 150,
 * 			"progress": 0,
 * 			"reward": [{
 * 				"type": 9,
 * 				"skin_id": 30138,
 * 				"num": 10,
 * 				"name": "樱花甜点",
 * 				"imgurl": "https://sca.tcsdzz.com/snake_gift_1648470168.png"
 * 			}],
 * 			"show_state": 0,
 * 			"start_desc": "05.28",
 * 			"state        1
 * 		}, {
 * 			"desc": "5.28后，无尽游戏本局长度达到50000分后概率遇到",
 * 			"floating_name": "大肠包小肠",
 * 			"id": 4,
 * 			"imgurl": "https://sca.tcsdzz.com/snake_file_1681714949_ttzi.png",
 * 			"is_open": 0,
 * 			"is_rare": 1,
 * 			"name": "台湾",
 * 			"need_num": 50,
 * 			"progress": 0,
 * 			"reward": [{
 * 				"type": 9,
 * 				"skin_id": 30120,
 * 				"num": 3,
 * 				"name": "深海珍珠",
 * 				"imgurl": "https://sca.tcsdzz.com/snake_gift_1632377817.png"
 * 			}],
 * 			"show_state": 0,
 * 			"start_desc": "05.28",
 * 			"state        1
 * 		}, {
 * 			"desc": "5.28后，玩无尽游戏概率遇到",
 * 			"floating_name": "咖喱鱼蛋",
 * 			"id": 5,
 * 			"imgurl": "https://sca.tcsdzz.com/snake_file_1681714945_txfj.png",
 * 			"is_open": 0,
 * 			"is_rare": 0,
 * 			"name": "香港",
 * 			"need_num": 88,
 * 			"progress": 0,
 * 			"reward": [{
 * 				"type": 7,
 * 				"skin_id": 20057,
 * 				"num": 1,
 * 				"name": "限时返利券（1日）",
 * 				"imgurl": "https://sca.tcsdzz.com/snake_prop_1568261288.png"
 * 			}],
 * 			"show_state": 0,
 * 			"start_desc": "05.28",
 * 			"state        1
 * 		}, {
 * 			"desc": "5.28后，玩无尽游戏概率遇到",
 * 			"floating_name": "蛋挞",
 * 			"id": 6,
 * 			"imgurl": "https://sca.tcsdzz.com/snake_file_1681714953_tjjx.png",
 * 			"is_open": 0,
 * 			"is_rare": 0,
 * 			"name": "澳门",
 * 			"need_num": 100,
 * 			"progress": 0,
 * 			"reward": [{
 * 				"type": 9,
 * 				"skin_id": 30127,
 * 				"num": 10,
 * 				"name": "爱的便当",
 * 				"imgurl": "https://sca.tcsdzz.com/snake_gift_1637033377.png"
 * 			}],
 * 			"show_state": 0,
 * 			"start_desc": "05.28",
 * 			"state        1
 * 		}, {
 * 			"desc": "不止属于秋天的奶茶",
 * 			"floating_name": "珍珠奶茶",
 * 			"id": 101,
 * 			"imgurl": "https://sca.tcsdzz.com/snake_file_1681715817_tfar.png",
 * 			"is_open": 1,
 * 			"is_rare": 1,
 * 			"name": "彩蛋美食1",
 * 			"need_num": 1000,
 * 			"progress": 1000,
 * 			"reward": [{
 * 				"type": 5,
 * 				"skin_id": 11248,
 * 				"num": 1,
 * 				"name": "珍珠奶茶",
 * 				"imgurl": "https://sca.tcsdzz.com/snake_file_1682304445_tsld.png"
 * 			}],
 * 			"show_state": 0,
 * 			"start_desc": "",
 * 			"state        3
 * 		}, {
 * 			"desc": "不会吧？还有人没有收集到草莓塔吗？",
 * 			"floating_name": "草莓塔",
 * 			"id": 102,
 * 			"imgurl": "https://sca.tcsdzz.com/snake_file_1682255172_tqem.png",
 * 			"is_open": 1,
 * 			"is_rare": 1,
 * 			"name": "彩蛋美食2",
 * 			"need_num": 150,
 * 			"progress": 150,
 * 			"reward": [{
 * 				"type": 5,
 * 				"skin_id": 11242,
 * 				"num": 1,
 * 				"name": "草莓塔",
 * 				"imgurl": "https://sca.tcsdzz.com/snake_file_1682303554_tzvn.png"
 * 			}],
 * 			"show_state": 0,
 * 			"start_desc": "",
 * 			"state        3
 * 		}, {
 * 			"desc": "有时候会很倒霉",
 * 			"floating_name": "蓝瘦香菇",
 * 			"id": 103,
 * 			"imgurl": "https://sca.tcsdzz.com/snake_file_1681715826_twqc.png",
 * 			"is_open": 1,
 * 			"is_rare": 1,
 * 			"name": "彩蛋美食3",
 * 			"need_num": 50,
 * 			"progress": 50,
 * 			"reward": [{
 * 				"type": 5,
 * 				"skin_id": 11244,
 * 				"num": 1,
 * 				"name": "蓝瘦香菇",
 * 				"imgurl": "https://sca.tcsdzz.com/snake_file_1682303861_txsv.png"
 * 			}],
 * 			"show_state": 0,
 * 			"start_desc": "",
 * 			"state        3
 * 		}, {
 * 			"desc": "柠檬爪，抓钱手，祝你发财，我的朋友",
 * 			"floating_name": "柠檬鸡爪",
 * 			"id": 104,
 * 			"imgurl": "https://sca.tcsdzz.com/snake_file_1681715854_tuze.png",
 * 			"is_open": 1,
 * 			"is_rare": 1,
 * 			"name": "彩蛋美食4",
 * 			"need_num": 50,
 * 			"progress": 50,
 * 			"reward": [{
 * 				"type": 5,
 * 				"skin_id": 11246,
 * 				"num": 1,
 * 				"name": "柠檬鸡爪",
 * 				"imgurl": "https://sca.tcsdzz.com/snake_file_1682304211_ticw.png"
 * 			}],
 * 			"show_state": 0,
 * 			"start_desc": "",
 * 			"state        3
 * 		}, {
 * 			"desc": "没有什么烦恼是一顿烤串解决不了的",
 * 			"floating_name": "烤串",
 * 			"id": 105,
 * 			"imgurl": "https://sca.tcsdzz.com/snake_file_1682255182_tfpo.png",
 * 			"is_open": 1,
 * 			"is_rare": 1,
 * 			"name": "彩蛋美食5",
 * 			"need_num": 120,
 * 			"progress": 21,
 * 			"reward": [{
 * 				"type": 5,
 * 				"skin_id": 11249,
 * 				"num": 1,
 * 				"name": "烤串",
 * 				"imgurl": "https://sca.tcsdzz.com/snake_file_1682306367_tsjf.png"
 * 			}],
 * 			"show_state": 0,
 * 			"start_desc": "",
 * 			"state        1
 * 		}],
 * 		"illus_extra": [{
 * 			"id": 20001,
 * 			"illus_list": [10001, 10002, 10003],
 * 			"imgurl": "https://sca.tcsdzz.com/snake_file_1682412869_tgjh.png",
 * 			"name": "皮肤：中华食神",
 * 			"reward": [{
 * 				"type": 3,
 * 				"skin_id": 1197,
 * 				"num": 1,
 * 				"name": "",
 * 				"imgurl": "https://sca.tcsdzz.com/snake_file_1682308502_thkc.png"
 * 			}],
 * 			"state        3
 * 		}, {
 * 			"id": 20002,
 * 			"illus_list": [10004, 10005, 10006, 10007, 10008],
 * 			"imgurl": "https://sca.tcsdzz.com/snake_file_1682415039_tbgy.png",
 * 			"name": "主页装扮:食神降临",
 * 			"reward": [{
 * 				"type": 40,
 * 				"skin_id": 220021,
 * 				"num": 1,
 * 				"name": "",
 * 				"imgurl": "https://sca.tcsdzz.com/snake_file_1682308628_tgkm.png"
 * 			}],
 * 			"state        1
 * 		}],
 * 		"illustrate": [{
 * 			"detail": [{
 * 				"name": "河南",
 * 				"state": 3,
 * 				"sub_id": 23
 * 			}, {
 * 				"name": "湖北",
 * 				"state": 3,
 * 				"sub_id": 24
 * 			}, {
 * 				"name": "湖南",
 * 				"state": 3,
 * 				"sub_id": 25
 * 			}],
 * 			"id": 10001,
 * 			"imgurl": "",
 * 			"is_open": 1,
 * 			"name": "华中",
 * 			"reward": [{
 * 				"type": 30,
 * 				"skin_id": 160545,
 * 				"num": 1,
 * 				"name": "",
 * 				"imgurl": "https://sca.tcsdzz.com/snake_file_1682254647_tbma.png",
 * 				"use_info": "5184000"
 * 			}],
 * 			"show_state": 0,
 * 			"start_desc": "",
 * 			"state        3
 * 		}, {
 * 			"detail": [{
 * 				"name": "江苏",
 * 				"state": 3,
 * 				"sub_id": 12
 * 			}, {
 * 				"name": "浙江",
 * 				"state": 3,
 * 				"sub_id": 13
 * 			}, {
 * 				"name": "安徽",
 * 				"state": 3,
 * 				"sub_id": 14
 * 			}, {
 * 				"name": "福建",
 * 				"state": 3,
 * 				"sub_id": 15
 * 			}, {
 * 				"name": "江西",
 * 				"state": 3,
 * 				"sub_id": 16
 * 			}, {
 * 				"name": "山东",
 * 				"state": 3,
 * 				"sub_id": 17
 * 			}, {
 * 				"name": "上海",
 * 				"state": 3,
 * 				"sub_id": 18
 * 			}],
 * 			"id": 10002,
 * 			"imgurl": "",
 * 			"is_open": 1,
 * 			"name": "华东",
 * 			"reward": [{
 * 				"type": 30,
 * 				"skin_id": 160543,
 * 				"num": 1,
 * 				"name": "",
 * 				"imgurl": "https://sca.tcsdzz.com/snake_file_1682254412_tsnf.png",
 * 				"use_info": "5184000"
 * 			}],
 * 			"show_state": 0,
 * 			"start_desc": "",
 * 			"state        3
 * 		}, {
 * 			"detail": [{
 * 				"name": "广东",
 * 				"state": 3,
 * 				"sub_id": 19
 * 			}, {
 * 				"name": "海南",
 * 				"state": 3,
 * 				"sub_id": 20
 * 			}, {
 * 				"name": "广西",
 * 				"state": 3,
 * 				"sub_id": 22
 * 			}],
 * 			"id": 10003,
 * 			"imgurl": "",
 * 			"is_open": 1,
 * 			"name": "华南",
 * 			"reward": [{
 * 				"type": 30,
 * 				"skin_id": 160544,
 * 				"num": 1,
 * 				"name": "",
 * 				"imgurl": "https://sca.tcsdzz.com/snake_file_1682254565_toiw.png",
 * 				"use_info": "5184000"
 * 			}],
 * 			"show_state": 0,
 * 			"start_desc": "",
 * 			"state        3
 * 		}, {
 * 			"detail": [{
 * 				"name": "河北",
 * 				"state": 1,
 * 				"sub_id": 7
 * 			}, {
 * 				"name": "山西",
 * 				"state": 1,
 * 				"sub_id": 8
 * 			}, {
 * 				"name": "内蒙古",
 * 				"state": 1,
 * 				"sub_id": 9
 * 			}, {
 * 				"name": "北京",
 * 				"state": 1,
 * 				"sub_id": 10
 * 			}, {
 * 				"name": "天津",
 * 				"state": 1,
 * 				"sub_id": 11
 * 			}],
 * 			"id": 10004,
 * 			"imgurl": "",
 * 			"is_open": 1,
 * 			"name": "华北",
 * 			"reward": [{
 * 				"type": 30,
 * 				"skin_id": 160540,
 * 				"num": 1,
 * 				"name": "",
 * 				"imgurl": "https://sca.tcsdzz.com/snake_file_1682254082_ticz.png",
 * 				"use_info": "5184000"
 * 			}],
 * 			"show_state": 0,
 * 			"start_desc": "",
 * 			"state        1
 * 		}, {
 * 			"detail": [{
 * 				"name": "黑龙江",
 * 				"state": 1,
 * 				"sub_id": 1
 * 			}, {
 * 				"name": "吉林",
 * 				"state": 1,
 * 				"sub_id": 2
 * 			}, {
 * 				"name": "辽宁",
 * 				"state": 1,
 * 				"sub_id": 3
 * 			}],
 * 			"id": 10005,
 * 			"imgurl": "",
 * 			"is_open": 1,
 * 			"name": "东北",
 * 			"reward": [{
 * 				"type": 30,
 * 				"skin_id": 160541,
 * 				"num": 1,
 * 				"name": "",
 * 				"imgurl": "https://sca.tcsdzz.com/snake_file_1682254326_tgna.png",
 * 				"use_info": "5184000"
 * 			}],
 * 			"show_state": 0,
 * 			"start_desc": "",
 * 			"state        1
 * 		}, {
 * 			"detail": [{
 * 				"name": "贵州",
 * 				"state": 1,
 * 				"sub_id": 31
 * 			}, {
 * 				"name": "云南",
 * 				"state": 1,
 * 				"sub_id": 32
 * 			}, {
 * 				"name": "西藏",
 * 				"state": 1,
 * 				"sub_id": 33
 * 			}, {
 * 				"name": "重庆",
 * 				"state": 1,
 * 				"sub_id": 34
 * 			}, {
 * 				"name": "四川",
 * 				"state": 1,
 * 				"sub_id": 21
 * 			}],
 * 			"id": 10006,
 * 			"imgurl": "",
 * 			"is_open": 1,
 * 			"name": "西南",
 * 			"reward": [{
 * 				"type": 30,
 * 				"skin_id": 160547,
 * 				"num": 1,
 * 				"name": "",
 * 				"imgurl": "https://sca.tcsdzz.com/snake_file_1682254850_tgfo.png",
 * 				"use_info": "5184000"
 * 			}],
 * 			"show_state": 0,
 * 			"start_desc": "",
 * 			"state        1
 * 		}, {
 * 			"detail": [{
 * 				"name": "陕西",
 * 				"state": 1,
 * 				"sub_id": 26
 * 			}, {
 * 				"name": "甘肃",
 * 				"state": 1,
 * 				"sub_id": 27
 * 			}, {
 * 				"name": "青海",
 * 				"state": 1,
 * 				"sub_id": 28
 * 			}, {
 * 				"name": "宁夏",
 * 				"state": 1,
 * 				"sub_id": 29
 * 			}, {
 * 				"name": "新疆",
 * 				"state": 1,
 * 				"sub_id": 30
 * 			}],
 * 			"id": 10007,
 * 			"imgurl": "",
 * 			"is_open": 0,
 * 			"name": "西北",
 * 			"reward": [{
 * 				"type": 30,
 * 				"skin_id": 160546,
 * 				"num": 1,
 * 				"name": "",
 * 				"imgurl": "https://sca.tcsdzz.com/snake_file_1682254686_tnxs.png",
 * 				"use_info": "5184000"
 * 			}],
 * 			"show_state": 0,
 * 			"start_desc": "05.28",
 * 			"state        1
 * 		}, {
 * 			"detail": [{
 * 				"name": "台湾",
 * 				"state": 1,
 * 				"sub_id": 4
 * 			}, {
 * 				"name": "香港",
 * 				"state": 1,
 * 				"sub_id": 5
 * 			}, {
 * 				"name": "澳门",
 * 				"state": 1,
 * 				"sub_id": 6
 * 			}],
 * 			"id": 10008,
 * 			"imgurl": "",
 * 			"is_open": 0,
 * 			"name": "港澳台",
 * 			"reward": [{
 * 				"type": 30,
 * 				"skin_id": 160542,
 * 				"num": 1,
 * 				"name": "",
 * 				"imgurl": "https://sca.tcsdzz.com/snake_file_1682254362_tqrc.png",
 * 				"use_info": "5184000"
 * 			}],
 * 			"show_state": 0,
 * 			"start_desc": "05.28",
 * 			"state        1
 * 		}],
 * 		"intro": "A级皮肤免费拿",
 * 		"period": "4.28-6.15",
 * 		"title": "美食    战"
 * 	},
 * 	"message": "",
 * 	"time": 1684082895
 * }
 */
