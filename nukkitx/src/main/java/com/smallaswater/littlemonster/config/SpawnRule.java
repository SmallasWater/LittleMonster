package com.smallaswater.littlemonster.config;

import cn.nukkit.utils.Config;
import lombok.Data;

import java.util.*;

/**
 * 单刷怪点的刷新规则。
 * <p>
 * 支持两种类型：
 * <ul>
 *   <li>{@link Type#FIXED}：固定间隔刷新（秒），等价于旧版标量「刷怪间隔」。</li>
 *   <li>{@link Type#SCHEDULED}：定时刷新，按 ISO 星期（1=周一 .. 7=周日）+ HH:mm:ss 时刻触发。
 *       支持通配符「*」表示「所有星期」，具体星期可覆盖通配符默认值。</li>
 * </ul>
 * <p>
 * 旧版配置会在 {@link #load(Config)} 中自动升级，并写回配置文件（同时移除旧 key），
 * 保证一次性永久升级，向后兼容。
 *
 * @author LittleMonster
 */
@Data
public class SpawnRule {

    /** 规则类型 */
    public enum Type { FIXED, SCHEDULED }

    private static final String KEY_RULE = "刷新规则";
    private static final String KEY_TYPE = "类型";
    private static final String KEY_FIXED_INTERVAL = "固定间隔";
    private static final String KEY_SCHEDULED_TIMES = "定时时刻";

    /** 通配符星期 key：代表「所有星期」，可被具体星期覆盖 */
    private static final String WILDCARD_KEY = "*";

    /** 旧版标量「刷怪间隔」键名，升级后会被移除 */
    private static final String LEGACY_KEY_INTERVAL = "刷怪间隔";
    /** 旧版「定时星期」键名，升级后会被移除 */
    private static final String LEGACY_KEY_WEEKDAYS = "定时星期";

    private static final int DEFAULT_INTERVAL = 10;

    private Type type = Type.FIXED;
    /** fixed 模式：刷新间隔（秒），恒大于 0 */
    private int fixedInterval = DEFAULT_INTERVAL;
    /** scheduled 模式：通配符「*」的默认时刻列表，应用到所有未单独配置的星期 */
    private List<int[]> defaultTimes = new ArrayList<>();
    /** scheduled 模式：具体星期的覆盖配置（key 为 ISO 星期 1-7），优先于 {@link #defaultTimes} */
    private Map<Integer, List<int[]>> timesByWeekday = new TreeMap<>();
    /** {@link #defaultTimes} 的预排序快照（升序），加载后只读，避免每 tick 重复排序分配 */
    private List<int[]> sortedDefaultTimes = Collections.emptyList();
    /** {@link #timesByWeekday} 各星期时刻的预排序快照（升序），与原 map 同 key */
    private Map<Integer, List<int[]>> sortedTimesByWeekday = Collections.emptyMap();

    private SpawnRule() {}

    /**
     * 从 yml Config 解析规则。
     * <p>
     * 检测到以下情况会就地升级为新的「刷新规则」map 并写回文件，同时移除旧 key：
     * <ol>
     *   <li>yml 中无「刷新规则」键（旧版或缺失）——读取旧标量「刷怪间隔」并转为 FIXED 规则。</li>
     *   <li>「刷新规则」节点非 map 或解析异常——回退到旧标量或默认值。</li>
     *   <li>SCHEDULED 规则但「定时时刻」配置不完整——降级为 FIXED。</li>
     *   <li>旧版「定时星期」+「定时时刻」拆分式配置——按笛卡尔积语义逐星期升级
     *       （{@code 定时星期: [1,2,3]} → {@code {"1": [时刻], "2": [时刻], "3": [时刻]}}）。</li>
     * </ol>
     *
     * @param config 刷怪点配置
     * @return 始终非 null（兜底 fixed 10s）
     */
    public static SpawnRule load(Config config) {
        SpawnRule rule = new SpawnRule();
        boolean dirty = false;
        // 是否已从「刷新规则」map 成功解析到有效数据；为 true 时不再用旧拆分格式覆盖，
        // 避免用户已写新格式、又残留旧 key（如「定时时刻」同时被新旧两套消费）时丢配置。
        boolean resolvedFromMap = false;

        Object ruleNode = config.get(KEY_RULE);
        if (ruleNode instanceof Map) {
            try {
                applyMap(rule, (Map<?, ?>) ruleNode);
                resolvedFromMap = true;
            } catch (Exception e) {
                // 新格式损坏，回退到旧标量或默认值，并触发写回
                rule.type = Type.FIXED;
                rule.fixedInterval = sanitizeInterval(config.getInt(LEGACY_KEY_INTERVAL, DEFAULT_INTERVAL));
                dirty = true;
            }
        } else {
            // 缺失或非 map：升级旧标量
            rule.type = Type.FIXED;
            rule.fixedInterval = sanitizeInterval(config.getInt(LEGACY_KEY_INTERVAL, DEFAULT_INTERVAL));
            dirty = true;
        }

        // 兼容旧版「定时星期」+「定时时刻」拆分式配置：仅在未从新格式 map 解析到数据时合并，
        // 否则会用旧值覆盖用户在新格式里已经配置的 scheduled 时刻。
        Object legacyWeekdays = config.get(LEGACY_KEY_WEEKDAYS);
        Object legacyTimes = config.get(KEY_SCHEDULED_TIMES);
        if (!resolvedFromMap && legacyWeekdays != null && legacyTimes != null) {
            mergeLegacySplitFormat(rule, legacyWeekdays, legacyTimes);
            dirty = true;
        }
        // 即便已从 map 解析，旧 key（刷怪间隔/定时星期/顶层定时时刻）若仍残留也要清理：
        // 标记 dirty 让 writeBack 统一移除
        if (resolvedFromMap && (legacyWeekdays != null || legacyTimes != null
                || config.get(LEGACY_KEY_INTERVAL) != null)) {
            dirty = true;
        }

        // scheduled 配置不完整则降级为 fixed，避免运行期无触发点
        if (rule.type == Type.SCHEDULED && rule.defaultTimes.isEmpty() && rule.timesByWeekday.isEmpty()) {
            rule.type = Type.FIXED;
            if (rule.fixedInterval <= 0) {
                rule.fixedInterval = DEFAULT_INTERVAL;
            }
            dirty = true;
        }

        if (dirty && config != null) {
            writeBack(config, rule);
        }
        rule.buildSortedTimes();
        return rule;
    }

    private static void applyMap(SpawnRule rule, Map<?, ?> m) {
        Object typeRaw = m.get(KEY_TYPE);
        String typeStr = typeRaw == null ? "" : String.valueOf(typeRaw).trim().toLowerCase();
        if ("scheduled".equals(typeStr)) {
            rule.type = Type.SCHEDULED;
        } else {
            rule.type = Type.FIXED;
        }
        rule.fixedInterval = sanitizeInterval(asInt(m.get(KEY_FIXED_INTERVAL), DEFAULT_INTERVAL));
        parseTimesByWeekday(m.get(KEY_SCHEDULED_TIMES), rule);
    }

    /**
     * 解析「定时时刻」节点，分离通配符默认值与具体星期覆盖。
     * <p>支持三种写法：
     * <ul>
     *   <li>纯时刻列表 {@code ["20:00:00"]} → 作为通配符默认值，应用到所有 7 天。</li>
     *   <li>map {@code {"*": [...], "2": [...]}} → 通配符为默认，星期 2 覆盖。</li>
     *   <li>map 全部为具体星期 {@code {"1":[...], "3":[...]}} → 无默认，未列出的星期不触发。</li>
     * </ul>
     * <p>注意：YAML 中未加引号的数字 key（如 {@code 5: [...]}）会被解析为 {@code Integer} 而非
     * {@code String}，直接强转 {@code Map<String,Object>} 后迭代会在取 key 时抛
     * {@link ClassCastException}。这里用原始类型 {@code Map<?,?>} 迭代，再用
     * {@link String#valueOf(Object)} 取 key，兼容两种写法。
     */
    private static void parseTimesByWeekday(Object o, SpawnRule rule) {
        if (o == null) return;
        if (o instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) o;
            for (Map.Entry<?, ?> e : map.entrySet()) {
                List<int[]> times = parseTimeList(e.getValue());
                if (times.isEmpty()) continue;
                String key = e.getKey() == null ? "" : String.valueOf(e.getKey());
                if (isWildcardKey(key)) {
                    rule.defaultTimes = times;
                } else {
                    int wd = asInt(key, -1);
                    if (wd >= 1 && wd <= 7) {
                        rule.timesByWeekday.put(wd, times);
                    }
                }
            }
        } else {
            // 最简写法：纯时刻列表 → 通配符默认值
            rule.defaultTimes = parseTimeList(o);
        }
    }

    /** 通配符 key：代表「所有星期」，可被具体星期覆盖。支持 *、all、every、每天/每日 */
    private static boolean isWildcardKey(String key) {
        if (key == null) return false;
        String k = key.trim().toLowerCase();
        return "*".equals(key.trim()) || "all".equals(k) || "every".equals(k)
                || "每天".equals(key.trim()) || "每日".equals(key.trim());
    }

    /**
     * 合并旧版拆分式配置：把「定时星期 = [1,2,3]」+「定时时刻 = ["20:00","12:00"]」
     * 逐星期升级为 {@code {"1": [时刻], "2": [时刻], "3": [时刻]}}。
     * <p>旧格式中星期与时刻是笛卡尔积（每个允许的星期共享全部时刻），因此必须
     * 逐星期写入 {@link #timesByWeekday} 保持语义等价——<b>不能</b>并入通配符
     * {@code "*"}，否则会把限定的星期扩展到全部 7 天。
     */
    private static void mergeLegacySplitFormat(SpawnRule rule, Object weekdaysRaw, Object timesRaw) {
        List<Integer> weekdays = parseWeekdayList(weekdaysRaw);
        List<int[]> times = parseTimeList(timesRaw);
        if (weekdays.isEmpty() || times.isEmpty()) {
            return;
        }
        rule.type = Type.SCHEDULED;
        rule.defaultTimes = new ArrayList<>();
        rule.timesByWeekday.clear();
        for (int wd : weekdays) {
            rule.timesByWeekday.put(wd, cloneTimes(times));
        }
    }

    private static List<int[]> cloneTimes(List<int[]> src) {
        List<int[]> out = new ArrayList<>();
        for (int[] t : src) {
            out.add(t.clone());
        }
        return out;
    }

    private static int sanitizeInterval(int v) {
        return v > 0 ? v : DEFAULT_INTERVAL;
    }

    private static int asInt(Object o, int def) {
        if (o == null) return def;
        if (o instanceof Number) return ((Number) o).intValue();
        try {
            return Integer.parseInt(String.valueOf(o).trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static List<Integer> parseWeekdayList(Object o) {
        List<Integer> out = new ArrayList<>();
        if (o == null) return out;
        for (Object item : toList(o)) {
            int d = asInt(item, -1);
            if (d >= 1 && d <= 7) {
                out.add(d);
            }
        }
        return out;
    }

    private static List<int[]> parseTimeList(Object o) {
        List<int[]> out = new ArrayList<>();
        if (o == null) return out;
        for (Object item : toList(o)) {
            if (item == null) continue;
            int[] t = parseSingleTime(String.valueOf(item).trim());
            if (t != null) out.add(t);
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> toList(Object o) {
        if (o instanceof List) {
            return (List<Object>) o;
        }
        // 支持 "1,2,3" 或 "20:00:00,12:00:00" 形式
        List<Object> out = new ArrayList<>();
        String s = String.valueOf(o).trim();
        for (String p : s.split("[,\\s]+")) {
            if (!p.isEmpty()) out.add(p);
        }
        return out;
    }

    private static int[] parseSingleTime(String s) {
        if (s == null || s.isEmpty()) return null;
        String[] parts = s.split(":");
        if (parts.length < 1 || parts.length > 3) return null;
        try {
            int h = Integer.parseInt(parts[0].trim());
            int min = parts.length >= 2 ? Integer.parseInt(parts[1].trim()) : 0;
            int sec = parts.length >= 3 ? Integer.parseInt(parts[2].trim()) : 0;
            if (h < 0 || h > 23 || min < 0 || min > 59 || sec < 0 || sec > 59) return null;
            return new int[]{h, min, sec};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static List<String> serializeTimes(List<int[]> times) {
        List<String> out = new ArrayList<>();
        for (int[] t : times) {
            out.add(String.format("%02d:%02d:%02d", t[0], t[1], t[2]));
        }
        return out;
    }

    private static void writeBack(Config config, SpawnRule rule) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put(KEY_TYPE, rule.type == Type.FIXED ? "fixed" : "scheduled");
        out.put(KEY_FIXED_INTERVAL, rule.fixedInterval);

        // 序列化「定时时刻」：通配符默认值用 "*" key，具体星期覆盖按数字 key。
        // 只要解析到了实际时刻数据就按实际值写回——与规则类型无关，
        // 避免 FIXED 模式下用户预配的 scheduled 时刻被模板覆盖；
        // 完全没有时刻数据时才写模板，便于用户日后切换类型。
        if (!rule.defaultTimes.isEmpty() || !rule.timesByWeekday.isEmpty()) {
            if (rule.timesByWeekday.isEmpty()) {
                // 只有通配符默认值（最简写法）：序列化为纯列表而非 map，配置更紧凑
                out.put(KEY_SCHEDULED_TIMES, serializeTimes(rule.defaultTimes));
            } else {
                Map<String, Object> timesMap = new LinkedHashMap<>();
                if (!rule.defaultTimes.isEmpty()) {
                    timesMap.put(WILDCARD_KEY, serializeTimes(rule.defaultTimes));
                }
                for (Map.Entry<Integer, List<int[]>> e : rule.timesByWeekday.entrySet()) {
                    timesMap.put(String.valueOf(e.getKey()), serializeTimes(e.getValue()));
                }
                out.put(KEY_SCHEDULED_TIMES, timesMap);
            }
        } else {
            // fixed 模式仍写回一个空的 scheduled 模板，便于用户切换类型
            Map<String, Object> timesMap = new LinkedHashMap<>();
            timesMap.put(WILDCARD_KEY, Collections.singletonList("20:00:00"));
            out.put(KEY_SCHEDULED_TIMES, timesMap);
        }

        config.set(KEY_RULE, out);
        config.remove(LEGACY_KEY_INTERVAL);
        config.remove(LEGACY_KEY_WEEKDAYS);
        // 旧版顶层「定时时刻」与新格式 map 内同名字段语义重合；
        // 升级时把顶层残留一并移除，避免新旧两份并存（map 内的值已由 out 写入）。
        config.remove(KEY_SCHEDULED_TIMES);
        config.save();
    }

    /**
     * 获取指定 ISO 星期（1-7）的有效触发时刻列表（已预排序）：优先具体星期覆盖，
     * 否则回退通配符默认值。
     */
    private List<int[]> getSortedTimesForWeekday(int isoWeekday) {
        List<int[]> specific = sortedTimesByWeekday.get(isoWeekday);
        if (specific != null && !specific.isEmpty()) {
            return specific;
        }
        return sortedDefaultTimes;
    }

    /**
     * 预排序时刻列表并缓存到 {@link #sortedDefaultTimes} / {@link #sortedTimesByWeekday}。
     * <p>时刻列表在加载后不变，运行期 {@link #nextTriggerSeconds} 每 tick 都需要按升序
     * 扫描——若每次现排会持续产生 {@code ArrayList} 分配与排序开销（多个刷怪点累积成
     * 稳定 GC 压力）。这里在加载完成后一次性构建只读的有序快照。
     */
    private void buildSortedTimes() {
        sortedDefaultTimes = sortTimes(defaultTimes);
        Map<Integer, List<int[]>> sorted = new TreeMap<>();
        for (Map.Entry<Integer, List<int[]>> e : timesByWeekday.entrySet()) {
            sorted.put(e.getKey(), sortTimes(e.getValue()));
        }
        sortedTimesByWeekday = sorted;
    }

    private static List<int[]> sortTimes(List<int[]> times) {
        if (times.isEmpty()) {
            return Collections.emptyList();
        }
        List<int[]> sorted = new ArrayList<>(times);
        Collections.sort(sorted, new Comparator<int[]>() {
            @Override
            public int compare(int[] a, int[] b) {
                if (a[0] != b[0]) return a[0] - b[0];
                if (a[1] != b[1]) return a[1] - b[1];
                return a[2] - b[2];
            }
        });
        return sorted;
    }

    /**
     * 计算下一次触发距离当前时刻的剩余秒数。
     * <ul>
     *   <li>FIXED 模式：返回 {@link #fixedInterval}。</li>
     *   <li>SCHEDULED 模式：用 {@link Calendar} 从当前时刻向后扫描，找到第一个匹配星期（在
     *       {@link #timesByWeekday} 或 {@link #defaultTimes} 中有配置）且时刻在该星期触发列表中
     *       的时间点，返回差值秒数。<b>错过不补刷</b>——当前时刻已过的今日时刻会被跳过，
     *       自动滚动到下一个允许的星期/时刻。</li>
     * </ul>
     * 服务器时区以 JVM 默认时区为准。
     *
     * @param nowMillis 当前毫秒时间戳（System.currentTimeMillis()）
     * @return 剩余秒数，恒 &gt;= 1；若 scheduled 无可触发项则回退到 {@link #fixedInterval}
     */
    public int nextTriggerSeconds(long nowMillis) {
        if (type == Type.FIXED) {
            return fixedInterval;
        }
        if (defaultTimes.isEmpty() && timesByWeekday.isEmpty()) {
            return fixedInterval;
        }

        Calendar now = Calendar.getInstance();
        now.setTimeInMillis(nowMillis);

        // 从今天起向后扫描，最多覆盖完整一周 + 缓冲
        for (int dayOffset = 0; dayOffset < 8; dayOffset++) {
            Calendar day = (Calendar) now.clone();
            day.add(Calendar.DAY_OF_MONTH, dayOffset);
            int iso = toIsoWeekday(day.get(Calendar.DAY_OF_WEEK));
            List<int[]> times = getSortedTimesForWeekday(iso);
            if (times == null || times.isEmpty()) {
                continue;
            }
            // 时刻列表已在加载时预排序，直接升序扫描
            for (int[] t : times) {
                Calendar candidate = (Calendar) day.clone();
                candidate.set(Calendar.HOUR_OF_DAY, t[0]);
                candidate.set(Calendar.MINUTE, t[1]);
                candidate.set(Calendar.SECOND, t[2]);
                candidate.set(Calendar.MILLISECOND, 0);
                long diffMs = candidate.getTimeInMillis() - nowMillis;
                if (diffMs > 0L) {
                    // 向上取整，避免亚秒级误差导致少算 1 秒
                    long diffSec = (diffMs + 999L) / 1000L;
                    return Math.max(1, (int) diffSec);
                }
                // 已过的今日时刻跳过（错过不补刷）
            }
        }
        // 理论上不可达（8 天内必有匹配），兜底
        return fixedInterval;
    }

    /** Calendar.DAY_OF_WEEK (SUN=1..SAT=7) → ISO 1=周一..7=周日 */
    private static int toIsoWeekday(int calendarDow) {
        return calendarDow == Calendar.SUNDAY ? 7 : calendarDow - 1;
    }
}
