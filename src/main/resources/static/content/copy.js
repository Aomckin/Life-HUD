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

export const statusLabels = {ACTIVE: "进行中", PAUSED: "暂停", COMPLETED: "已完成", ARCHIVED: "已归档"};
export const milestoneStatusLabels = {PENDING: "待完成", COMPLETED: "已完成", ARCHIVED: "已归档"};

export const directionCopy = {
  emptyActive: "还没有写下想追逐的东西。",
  emptyFiltered: "这个状态下还没有梦想。",
  createFirst: "创建第一个梦想",
  newDream: "＋ 新的梦想",
  backToList: "← 返回梦想列表",
  meaningLabel: "为什么想做到",
  meaningPlaceholder: "一句话记住这件事对你的意义",
  titleLabel: "标题",
  descriptionLabel: "描述（可选）",
  targetDateLabel: "目标日期（可选）",
  coverLabel: "封面（可选）",
  noteLabel: "笔记（可选）",
  statusLabel: "状态",
  save: "保存",
  cancel: "取消",
  edit: "编辑",
  complete: "完成",
  pause: "暂停",
  resume: "恢复",
  archive: "归档",
  archiveConfirm: "归档这个梦想？它不会出现在进行中列表，但所有记录都会保留。",
  goalsTitle: "方向 · Goals",
  newGoal: "＋ 新方向",
  goalTitle: "方向标题",
  milestonesTitle: "里程碑",
  newMilestone: "＋ 里程碑",
  milestoneTitle: "里程碑标题",
  linkedTasks: "关联任务",
  noLinkedTasks: "还没有关联到这个梦想的任务。",
  goalDone: "完成",
  delete: "删除",
  deleteConfirm: "删除后无法恢复，确定继续？"
};

export const ritualCopy = {
  empty: "还没有属于你的仪式。",
  createFirst: "创建仪式",
  newRitual: "＋ 新仪式",
  runnableTitle: "当前可执行",
  disabledTitle: "已停用",
  recentTitle: "最近执行",
  nameLabel: "仪式名称",
  descriptionLabel: "一句描述",
  categoryLabel: "类别（晨间 / 夜间 / 创作…）",
  triggerTimeLabel: "触发时间（可选）",
  enabledLabel: "启用",
  stepsTitle: "步骤",
  addStep: "＋ 添加步骤",
  stepTypeLabel: "类型",
  stepTitleLabel: "标题",
  stepContentLabel: "内容 / 提示",
  stepDurationLabel: "秒",
  stepUrlLabel: "链接（可选）",
  stepRequiredLabel: "必做",
  removeStep: "移除",
  start: "开始",
  edit: "编辑",
  disable: "停用",
  enableAction: "启用",
  history: "历史",
  deleteConfirm: "删除这个仪式？执行历史也会一并删除。",
  runnerProgress: (index, total) => `${index + 1} / ${total}`,
  stepDone: "完成这一步",
  skipStep: "跳过",
  prevStep: "上一步",
  nextStep: "下一步",
  finishRitual: "完成仪式",
  cancelRitual: "取消仪式",
  cancelConfirm: "取消这次执行？已做的记录会保留为“已取消”。",
  requiredPending: "还有必做步骤没有完成，确定直接完成仪式？",
  timerRunning: "计时中…",
  timerStart: "开始计时",
  notePlaceholder: "写点什么…",
  openLink: "打开链接",
  executionsEmpty: "还没有执行记录。",
  noSteps: "这个仪式还没有步骤，编辑后添加。"
};

export const nowCopy = {
  empty: "「现在。」还没有留下内容。从这一刻开始记录。",
  editStage: "编辑阶段",
  stageTitleLabel: "阶段",
  stageTitlePlaceholder: "2026 盛夏",
  themeLabel: "主题",
  themePlaceholder: "热烈、开发、独居、秋招",
  save: "保存当前状态",
  saved: "「现在。」已更新",
  snapshotButton: "保存阶段快照",
  snapshotDone: "这一刻已经留下来了",
  playlistTitle: "「现在。」歌单",
  playlistHint: "此刻最喜欢的十首歌",
  bgAdd: "＋ 添加背景",
  bgReplace: "更换背景",
  bgRemove: "移除背景",
  playedLabel: n => `听了 ${n} 次`,
  playCountLabel: "听歌次数",
  noteEditorLabel: "这一阶段的一句话",
  noteEditorPlaceholder: "八月末…… 夏日落幕感……",
  editorTitle: "编辑歌曲",
  relayout: "重新排版",
  addSong: "＋ 添加歌曲",
  songReplace: "替换",
  songRemove: "移除",
  unknownArtist: "未知艺术家",
  lists: {
    favoriteSongs: "最近最喜欢的歌",
    currentGames: "当前游戏",
    currentAnime: "当前番剧",
    currentBooks: "当前书"
  },
  addItemLabels: {
    currentGames: "＋ 添加当前游戏",
    currentAnime: "＋ 添加当前番剧",
    currentBooks: "＋ 添加当前书"
  },
  itemTitle: "标题",
  itemSubtitle: "补充（如 看到 EP.4）",
  itemNote: "备注（可选）",
  itemSave: "保存",
  itemCancel: "取消",
  currentDreams: "当前梦想",
  currentGoals: "当前方向",
  dreamsEmpty: "这一阶段还没有选择正在追逐的梦想",
  chooseDreams: "选择梦想",
  chooseGoals: "选择方向",
  done: "完成",
  quoteLabel: "最近喜欢的一句话",
  quotePlaceholder: "写下最近击中你的一句话",
  contentLabel: "此刻想说的话",
  contentPlaceholder: "此刻的状态、心情、在想的事……",
  edit: "编辑",
  images: "此刻的图片",
  uploadImage: "＋ 添加图片",
  removeImage: "移除",
  snapshotsTitle: "历史快照",
  snapshotsEmpty: "还没有阶段快照。当这一刻值得保存时，按下上面的按钮。",
  songsLabel: n => n ? `${n} 首歌` : "",
  snapshotView: "查看",
  snapshotDelete: "删除",
  snapshotDeleteConfirm: "删除这份历史快照？无法恢复。",
  readonlyTitle: "历史快照（只读）",
  backToCurrent: "← 返回「现在。」",
  audioLabel: "播放"
};

export const taskCopy = {
  title: "任务 · Tasks",
  dailyTitle: "每日任务",
  specialTitle: "特殊任务",
  directionLabel: "关联方向",
  unlinkLabel: "取消关联",
  editLink: "关联",
  linkTitle: "关联到方向",
  linkHint: "选择最深层即可，上层自动推导；全部留空则保持独立任务。",
  dreamLabel: "梦想",
  goalLabel: "方向（可选）",
  milestoneLabel: "里程碑（可选）",
  noDirection: "独立任务",
  doneBadge: "已完成",
  pendingBadge: "未完成",
  emptyTasks: "还没有可显示的任务。"
};
