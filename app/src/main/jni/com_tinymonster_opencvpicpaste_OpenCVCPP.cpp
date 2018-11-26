#include "com_tinymonster_opencvpicpaste_OpenCVCPP.h"
#include <vector>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/highgui/highgui.hpp>
#include <opencv2/stitching/stitcher.hpp>
using namespace cv;
using namespace std;
char FILEPATH[100]="/storage/emulated/0/panorama_stitched.jpg";
JNIEXPORT jint JNICALL Java_com_tinymonster_opencvpicpaste_OpenCVCPP_StitchPanorama
  (JNIEnv * env, jclass obj, jobjectArray images, jint size, jlong resultMatAddr){
  jint resultReturn=0;
  vector<Mat> clickedImages=vector<Mat>();
  Mat output_stitched=Mat();
  Mat& srcRes=*(Mat*)resultMatAddr, img;
  jclass clazz=(env)->FindClass("org/opencv/core/Mat");//调用java的Mat类
  jmethodID getNativeObjAddr=(env)->GetMethodID(clazz,"getNativeObjAddr","()J");//调用java的Mat类的方法
  for(int i=0;i<size;i++){
  jobject obj=(env->GetObjectArrayElement(images,i));//获取图片对象
  jlong result=(env)->CallLongMethod(obj,getNativeObjAddr,NULL);//调用java方法,返回MAT的nativeAddr
  img =*(Mat*) result;
  resize(img,img,Size(img.rows/10,img.cols/10));
  clickedImages.push_back(img);
  env->DeleteLocalRef(obj);//清除对象
  }
  //env->DeleteLocalRef(images);//清除对象
  Stitcher stitcher =Stitcher::createDefault();
  Stitcher::Status status=stitcher.stitch(clickedImages,output_stitched);
  output_stitched.copyTo(srcRes);
  if(status==Stitcher::OK){
  resultReturn=1;
  }else{
  resultReturn=0;
  }
  return resultReturn;
  }