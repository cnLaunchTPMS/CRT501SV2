package com.cnlaunch.crt501sv2util.bean

enum class LanguageEnum(
  //获取语言对应的索引值
  val index: Int,
  //获取区域车型的索引值
  val regionIndex: Int
) {
  // 定义语言和对应的索引值
  EN(0, 2),  // 英语
  CN(1, 3),  // 中文
  TW(2, 3),  // 台湾
  ES(3, 2),  // 西班牙语
  FR(4, 2),  // 法语
  DE(5, 2),  // 德语
  IT(6, 2),  // 意大利语
  RU(7, 2),  // 俄罗斯
  PT(8, 2),  // 葡萄牙语
  JA(9, 0),  // 日语
  KR(10, 0),  // 韩语
  NL(11, 2),  // 荷兰语
  PL(12, 2),  // 波兰语
  CS(13, 2),  // 捷克语
  SK(14, 2),  // 斯洛伐克语
  TR(15, 2),  // 土耳其语
  HU(16, 2),  // 匈牙利语
  AR(17, 0),  // 阿拉伯语
  US(0, 1);

}