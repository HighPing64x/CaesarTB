package com.caesar.toolbox.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.random.Random

// ============================================================
// 中文姓名生成器 — 丰富词库
// ============================================================

private val SURNAMES = listOf(
    "赵","钱","孙","李","周","吴","郑","王","冯","陈","褚","卫","蒋","沈","韩","杨","朱","秦","尤","许","何","吕","施","张","孔","曹","严","华","金","魏","陶","姜","戚","谢","邹","喻","柏","水","窦","章","云","苏","潘","葛","奚","范","彭","郎","鲁","韦","昌","马","苗","凤","花","方","俞","任","袁","柳","酆","鲍","史","唐","费","廉","岑","薛","雷","贺","倪","汤","滕","殷","罗","毕","郝","邬","安","常","乐","于","时","傅","皮","卞","齐","康","伍","余","元","卜","顾","孟","平","黄","和","穆","萧","尹","姚","邵","湛","汪","祁","毛","禹","狄","米","贝","明","臧","计","伏","成","戴","谈","宋","茅","庞","熊","纪","舒","屈","项","祝","董","梁","杜","阮","蓝","闵","席","季","麻","强","贾","路","娄","危","江","童","颜","郭","梅","盛","林","刁","钟","徐","邱","骆","高","夏","蔡","田","樊","胡","凌","霍","虞","万","支","柯","昝","管","卢","莫","经","房","裘","缪","干","解","应","宗","丁","宣","贲","邓","郁","单","杭","洪","包","诸","左","石","崔","吉","钮","龚","程","嵇","邢","滑","裴","陆","荣","翁","荀","羊","於","惠","甄","曲","家","封","芮","羿","储","靳","汲","邴","糜","松","井","段","富","巫","乌","焦","巴","弓","牧","隗","山","谷","车","侯","宓","蓬","全","郗","班","仰","秋","仲","伊","宫","宁","仇","栾","暴","甘","钭","厉","戎","祖","武","符","刘","景","詹","束","龙","叶","幸","司","韶","郜","黎","蓟","薄","印","白","怀","蒲","邰","从","鄂","索","咸","籍","赖","卓","蔺","屠","蒙","池","乔","阴","鬱","胥","能","苍","双","闻","莘","党","翟","谭","贡","劳","逄","姬","申","扶","堵","冉","宰","郦","雍","卻","璩","桑","桂","濮","牛","寿","通","边","扈","燕","冀","郏","浦","尚","农","温","别","庄","晏","柴","瞿","阎","充","慕","连","茹","习","宦","艾","鱼","容","向","古","易","慎","戈","廖","庾","终","暨","居","衡","步","都","耿","满","弘","匡","国","文","寇","广","禄","阙","东","欧","殳","沃","利","蔚","越","夔","隆","师","巩","厍","聂","晁","勾","敖","融","冷","訾","辛","阚","那","简","饶","空","曾","毋","沙","乜","养","鞠","须","丰","巢","关","蒯","相","查","后","荆","红","游","竺","权","逯","盖","益","桓","公"
)

private val MALE_NAMES = listOf(
    "伟","强","磊","洋","勇","军","杰","涛","明","超","辉","鹏","浩","亮","刚","健","平","峰","林","飞",
    "斌","宇","文","波","毅","俊","帅","鑫","旭","晨","龙","威","宁","华","嘉","睿","毅","博","楠","瑞",
    "子轩","宇轩","浩然","子涵","雨泽","梓豪","一鸣","天佑","文博","明哲","思远","志远","修杰","承恩","鸿飞",
    "翰林","景行","玉树","临风","逸飞","星河","云天","清风","明月","凌云","四海","博文","承志","德才",
    "学成","博文","广才","智勇","仁杰","信诚","义方","礼和","书翰","墨林","砚耕","竹君","松涛","柏年",
    "启明","建国","卫东","向东","志强","建华","国强","振华","新华","兴华","耀华","光华"
)

private val FEMALE_NAMES = listOf(
    "芳","敏","静","丽","秀","娟","英","华","慧","巧","美","娜","淑","惠","珠","翠","雅","芝","玉","萍",
    "红","娥","玲","芬","燕","彩","春","菊","兰","凤","洁","梅","琳","素","云","莲","真","环","雪","荣",
    "雨涵","梓萱","诗涵","欣怡","梓涵","梦琪","语嫣","晓婷","思雨","佳怡","雅静","婉清","若兰","如雪",
    "冰清","玉洁","秋月","春桃","夏荷","冬梅","晓燕","紫薇","含烟","如风","若云","似水","如画","如诗",
    "晓红","明艳","秀丽","文雅","端丽","清秀","娇艳","妩媚","婵娟","娥眉","凤仪","桃红","柳绿","花蕊"
)

private val NEUTRAL_NAMES = listOf(
    "辰","逸","泽","宇","然","诺","安","乐","欣","悦","文","艺","思","远","明","清","宁","静","晨","夕",
    "知","行","念","怀","期","望","信","义","仁","和","平","正","直","谦","恭"
)

private val SURNAME_PREFIX = listOf(
    "欧阳","太史","端木","上官","司马","东方","独孤","南宫","夏侯","诸葛","尉迟","皇甫","慕容","宇文",
    "令狐","申屠","公孙","轩辕","钟离","长孙","慕容","鲜于","闾丘","司徒","司空","亓官","司寇","巫马",
    "公西","壤驷","公良","漆雕","乐正","宰父","谷梁","拓跋","夹谷","轩辕","赫连","澹台","公冶","宗政",
    "濮阳","淳于","单于","太叔","申屠","公孙","仲孙"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NameScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    var names by remember { mutableStateOf<List<String>>(emptyList()) }
    var gender by remember { mutableStateOf("random") }
    var count by remember { mutableStateOf(20) }
    var includeRare by remember { mutableStateOf(false) }
    var includeCompound by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current

    Scaffold(modifier = modifier, containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(title = { Text("姓名生成", fontWeight = FontWeight.Bold) },
                navigationIcon = { TextButton(onClick = onBack) { Text("← 返回") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background))
        }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {

            // 性别选择
            Text("性别倾向", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("random" to "随机", "male" to "男性", "female" to "女性").forEach { (k, v) ->
                    FilterChip(selected = gender == k, onClick = { gender = k },
                        label = { Text(v, style = MaterialTheme.typography.labelSmall) })
                }
            }

            // 生成数量
            OutlinedTextField(value = count.toString(), onValueChange = { count = it.filter { c -> c.isDigit() }.toIntOrNull()?.coerceIn(1, 100) ?: 20 },
                label = { Text("生成数量 (1-100)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

            // 选项
            Row {
                Checkbox(checked = includeRare, onCheckedChange = { includeRare = it })
                Text("包含复姓", modifier = Modifier.align(Alignment.CenterVertically), style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.width(12.dp))
                Checkbox(checked = includeCompound, onCheckedChange = { includeCompound = it })
                Text("双字名", modifier = Modifier.align(Alignment.CenterVertically), style = MaterialTheme.typography.bodySmall)
            }

            Button(onClick = { names = generateNames(gender, count, includeRare, includeCompound) },
                modifier = Modifier.fillMaxWidth()) { Text("生成姓名") }

            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("${names.size} 个结果", style = MaterialTheme.typography.labelMedium)
                if (names.isNotEmpty()) TextButton(onClick = {
                    clipboard.setText(AnnotatedString(names.joinToString("\n")))
                }) { Text("复制全部") }
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.heightIn(max = 400.dp)) {
                items(names) { name ->
                    Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceVariant) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(name, fontWeight = FontWeight.Medium)
                            IconButton(onClick = { clipboard.setText(AnnotatedString(name)) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Outlined.ContentCopy, "复制", Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun generateNames(gender: String, count: Int, rare: Boolean, compound: Boolean): List<String> {
    val surnames = if (rare) SURNAMES + SURNAME_PREFIX else SURNAMES
    val names = mutableSetOf<String>()
    val rng = Random(System.nanoTime())
    while (names.size < count && names.size < 5000) {
        val surname = surnames[rng.nextInt(surnames.size)]
        val given = when (gender) {
            "male" -> pickGiven(rng, MALE_NAMES, compound, NEUTRAL_NAMES)
            "female" -> pickGiven(rng, FEMALE_NAMES, compound, NEUTRAL_NAMES)
            else -> {
                val pool = if (rng.nextBoolean()) MALE_NAMES else FEMALE_NAMES
                pickGiven(rng, pool, compound, NEUTRAL_NAMES)
            }
        }
        names.add(surname + given)
    }
    return names.shuffled(rng).take(count)
}

private fun pickGiven(rng: Random, pool: List<String>, compound: Boolean, neutral: List<String>): String {
    if (compound && rng.nextFloat() < 0.6f) {
        return pool.filter { it.length == 2 }.ifEmpty { pool }[rng.nextInt(pool.filter { it.length == 2 }.ifEmpty { pool }.size)]
    }
    val first = pool[rng.nextInt(pool.size)]
    return if (first.length == 2 || rng.nextFloat() < 0.7f) first else {
        first + neutral[rng.nextInt(neutral.size)]
    }
}
