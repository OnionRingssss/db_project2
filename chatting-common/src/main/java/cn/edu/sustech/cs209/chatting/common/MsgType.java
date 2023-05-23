package cn.edu.sustech.cs209.chatting.common;

public enum MsgType {
    COMMAND,TALK,REQ,TALKINGTO,GROUP_CREATE,G_TALK,G_COMMAND,G_REQ,G_CREATEGCONTROLLER,
    EXIT,EXIT_NO_KEEP,SERVER_EXIT,NOT_ALLOW_LOGIN,R_FIAL,EXIT_FROM_GROUP,FILE,

    client_login,client_register,client_register_success,client_register_reject,client_login_reject,client_login_success,
    dian_zan,shou_cang,zhuan_fa,guan_zhu,qu_xiao_guan_zhu,
    cha_kan_dian_zan, cha_kan_shou_cang, cha_kan_zhuan_fa, cha_kan_guan_zhu,
    fa_bu_tie_zi,re_fa_bu_tie_zi_content,re_fa_bu_tie_zi_city,re_fa_bu_tie_zi_category,
    hui_fu_tie_zi,
    hui_fu_hui_fu,
    cha_kan_ta_ren_fa_bu,
    cha_kan_fa_bu,
    cha_kan_hui_fu,
    cha_kan_er_ji_hui_fu,
    multi_search, // 多参数搜索
    ping_bi;
}
