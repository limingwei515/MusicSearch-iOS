package com.linfeng.music.utils;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

import com.linfeng.music.R;

/**
 * 导航动画辅助类
 * 提供统一的Fragment切换动画，包含返回动画预测
 */
public class NavAnimationHelper {

    /**
     * 获取标准的Fragment切换动画参数
     */
    public static void addFragmentWithAnimation(
            FragmentActivity activity,
            Fragment fragment,
            String tag,
            int containerId,
            boolean addToBackStack) {
        FragmentTransaction transaction = activity.getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(
                        R.anim.slide_in_right,  // 进入：从右边滑入
                        R.anim.slide_out_left,  // 退出：向左边滑出
                        R.anim.slide_in_left,   // 返回：从左边滑入
                        R.anim.slide_out_right  // 返回退出：向右边滑出
                )
                .add(containerId, fragment, tag);
        
        if (addToBackStack) {
            transaction.addToBackStack(null);
        }
        
        transaction.commitAllowingStateLoss();
    }

    /**
     * 替换Fragment并添加返回栈
     */
    public static void replaceFragmentWithAnimation(
            FragmentActivity activity,
            Fragment fragment,
            String tag,
            int containerId) {
        activity.getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(
                        R.anim.slide_in_right,
                        R.anim.slide_out_left,
                        R.anim.slide_in_left,
                        R.anim.slide_out_right
                )
                .replace(containerId, fragment, tag)
                .addToBackStack(null)
                .commitAllowingStateLoss();
    }

    /**
     * 在Fragment内部切换Fragment
     */
    public static void addChildFragmentWithAnimation(
            Fragment parentFragment,
            Fragment fragment,
            String tag,
            int containerId,
            boolean addToBackStack) {
        FragmentTransaction transaction = parentFragment.getChildFragmentManager()
                .beginTransaction()
                .setCustomAnimations(
                        R.anim.slide_in_right,
                        R.anim.slide_out_left,
                        R.anim.slide_in_left,
                        R.anim.slide_out_right
                )
                .add(containerId, fragment, tag);
        
        if (addToBackStack) {
            transaction.addToBackStack(null);
        }
        
        transaction.commitAllowingStateLoss();
    }

    /**
     * 从右边进入动画
     */
    public static int[] getSlideRightAnimations() {
        return new int[]{
                R.anim.slide_in_right,
                R.anim.slide_out_left,
                R.anim.slide_in_left,
                R.anim.slide_out_right
        };
    }
}
