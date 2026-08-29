/**
 * Central UI copy and content limits for Life HUD pages.
 * Page-specific hero prose stays in its page; anything shared or tunable belongs here.
 */

export const modeLabels = {IRON_CURTAIN: "铁幕", POMODORO: "番茄", FREE: "自由专注"};

export const limits = {
  milestoneTitle: 120,
  milestoneDescription: 1000,
  milestoneMedia: 500,
  titleName: 40,
  titleDescription: 200
};

export const growthCopy = {
  energyCardHint: "在行动与生活之间流动",
  energyFlowEmpty: "今天的流动还没有开始",
  expAccumulating: "Energy 被真实消耗后留下的积累",
  expLongTermNote: "Energy 被实际使用后留下的长期积累"
};

export const entertainmentCategories = [
  ["GAME", "游戏"], ["ANIME", "看番"], ["MOVIE", "电影"], ["VIDEO", "视频"],
  ["SOCIAL", "社交"], ["OUTING", "外出娱乐"], ["OTHER", "其他"]
];

export const entertainmentCopy = {
  entryLabel: "＋ 记录娱乐",
  formTitle: "记录一次娱乐",
  categoryLabel: "类型",
  titleLabel: "做了什么",
  titlePlaceholder: "番剧/一场电影/出游……",
  durationLabel: "持续时间",
  costLabel: "Energy 消耗",
  costHint: "由你填写；Energy 不足时只按实际剩余结算",
  noteLabel: "备注（可选）",
  submit: "记录",
  save: "保存",
  cancel: "取消",
  updateButton: "编辑",
  deleteButton: "删除",
  deleteConfirm: "删除这条娱乐记录？Energy 与 EXP 历史不会自动回滚。",
  empty: "还没有娱乐记录。晚上认真玩过的，都可以留在这里。",
  energyHistoryTitle: "Energy History",
  energyHistoryEmpty: "Energy 的每次变化都会记录在这里。",
  recentTitle: "最近娱乐"
};
