package com.tinymonster.opencvpicpaste;

/**
 * Created by TinyMonster on 2018/5/25.
 */

public class OpenCVCPP{
    public static native int StitchPanorama(Object images[],int size,long addrSrcRes);//输入Mat数组，Mat数组大小，返回图片的Native地址
}
