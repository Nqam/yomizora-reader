package com.yomizora.reader.data.content

import com.yomizora.reader.domain.model.GrammarPoint
import com.yomizora.reader.domain.model.JlptLevel
import com.yomizora.reader.domain.model.ReadingMaterial

object GrammarCatalog {

    private data class GrammarRule(
        val point: GrammarPoint,
        val regex: Regex
    )

    // Ordered from MOST specific / longest constructions to most general so segment matching
    // always picks the exact construction rather than a shorter substring.
    private val rules: List<GrammarRule> = listOf(
        GrammarRule(
            GrammarPoint(
                id = "g_n1_wo_yoginakusareru",
                pattern = "～を余儀なくされる",
                level = JlptLevel.N1,
                titleRu = "Быть вынужденным (под давлением обстоятельств)",
                formula = "Существительное + を余儀なくされる / された",
                explanationRu = "Книжная конструкция, означающая, что из-за неизбежных обстоятельств приходится принять трудное решение или изменить планы.",
                exampleJp = "悪天候のため計画の変更を余儀なくされた。",
                exampleRu = "Из-за непогоды пришлось вынужденно изменить план."
            ),
            Regex("を余儀なくされ(る|た|て|ます|ました)")
        ),
        GrammarRule(
            GrammarPoint(
                id = "g_n1_zu_niwa_okanai",
                pattern = "～ずにはおかない",
                level = JlptLevel.N1,
                titleRu = "Неизбежно вызывать (эмоцию, реакцию)",
                formula = "Глагол (ない-основа) + ずにはおかない",
                explanationRu = "Выражает уверенность в том, что явление обязательно вызовет определённое чувство или действие у каждого.",
                exampleJp = "その言葉は聞く人の心を動かさずにはおかない。",
                exampleRu = "Эти слова неизбежно тронут сердце любого слушателя."
            ),
            Regex("ずにはおか(ない|なかった)")
        ),
        GrammarRule(
            GrammarPoint(
                id = "g_n1_nomi_narazu",
                pattern = "～のみならず",
                level = JlptLevel.N1,
                titleRu = "Не только..., но и...",
                formula = "Сущ. / Глаг. / Прил. (простая форма) + のみならず",
                explanationRu = "Формальный и книжный эквивалент だけでなく: подчёркивает, что сказанное выходит далеко за рамки первого примера.",
                exampleJp = "この技術は医療のみならず教育にも応用されている。",
                exampleRu = "Эта технология применяется не только в медицине, но и в образовании."
            ),
            Regex("のみならず")
        ),
        GrammarRule(
            GrammarPoint(
                id = "g_n1_naradewa",
                pattern = "～ならではの",
                level = JlptLevel.N1,
                titleRu = "Уникальный, присущий только...",
                formula = "Существительное + ならではの + Существительное",
                explanationRu = "Подчёркивает особое достоинство или атмосферу, возможную исключительно в данном месте или у данного человека.",
                exampleJp = "これは老舗ならではの繊細な味わいである。",
                exampleRu = "Это утончённый вкус, присущий только старинному заведению."
            ),
            Regex("ならではの")
        ),
        GrammarRule(
            GrammarPoint(
                id = "g_n1_wo_megutte",
                pattern = "～をめぐって",
                level = JlptLevel.N1,
                titleRu = "Вокруг (спора, вопроса, обсуждения)",
                formula = "Существительное + をめぐって / をめぐる",
                explanationRu = "Указывает на центральную тему, вокруг которой идут дискуссии, разногласия или исследования.",
                exampleJp = "新しい制度をめぐって活発な議論が続いている。",
                exampleRu = "Вокруг новой системы продолжаются оживлённые споры."
            ),
            Regex("をめぐ(って|る|り)")
        ),
        GrammarRule(
            GrammarPoint(
                id = "g_n2_zaru_wo_enai",
                pattern = "～ざるを得ない",
                level = JlptLevel.N2,
                titleRu = "Приходится, ничего не остаётся кроме как",
                formula = "Глагол (ない-основа) + ざるを得ない",
                explanationRu = "Означает, что даже если не хочется, обстоятельства вынуждают так поступить.",
                exampleJp = "予算が足りず、計画を縮小せざるを得ない。",
                exampleRu = "Бюджета не хватает, поэтому приходится сокращать план."
            ),
            Regex("ざるを得(ない|ません|なかった)")
        ),
        GrammarRule(
            GrammarPoint(
                id = "g_n2_ni_mo_kakawarazu",
                pattern = "～にもかかわらず",
                level = JlptLevel.N2,
                titleRu = "Несмотря на...",
                formula = "Сущ. / Прил. / Глаг. + にもかかわらず",
                explanationRu = "Формальное «несмотря на то, что...» — указывает на неожиданный результат вопреки условию.",
                exampleJp = "忙しいにもかかわらず、丁寧に対応してくれた。",
                exampleRu = "Несмотря на занятость, он отнёсся ко мне очень внимательно."
            ),
            Regex("にもかかわらず")
        ),
        GrammarRule(
            GrammarPoint(
                id = "g_n2_wake_niwa_ikanai",
                pattern = "～わけにはいかない",
                level = JlptLevel.N2,
                titleRu = "Нельзя себе позволить / невозможно",
                formula = "Глагол (словарная форма) + わけにはいかない",
                explanationRu = "Выражает моральный, социальный или логический запрет: «при всём желании так поступить нельзя».",
                exampleJp = "約束した以上、途中でやめるわけにはいかない。",
                exampleRu = "Раз уж пообещал, бросить на полпути нельзя."
            ),
            Regex("わけにはいか(ない|ません|なかった)")
        ),
        GrammarRule(
            GrammarPoint(
                id = "g_n2_ni_tomonatte",
                pattern = "～に伴って",
                level = JlptLevel.N2,
                titleRu = "По мере того как / вместе с...",
                formula = "Существительное / Глагол + に伴って / に伴う",
                explanationRu = "Показывает, что одно изменение закономерно влечёт за собой другое.",
                exampleJp = "高齢化に伴って、医療の需要が増えている。",
                exampleRu = "По мере старения населения растёт спрос на медицину."
            ),
            Regex("に伴(って|い|う)")
        ),
        GrammarRule(
            GrammarPoint(
                id = "g_n2_tsutsu_aru",
                pattern = "～つつある",
                level = JlptLevel.N2,
                titleRu = "Постепенно происходит / находится в процессе",
                formula = "Глагол (ます-основа) + つつある",
                explanationRu = "Книжно-публицистическая форма для описания устойчивой тенденции или постепенного изменения.",
                exampleJp = "人々の働き方は大きく変わりつつある。",
                exampleRu = "Способы работы людей постепенно и заметно меняются."
            ),
            Regex("つつあ(る|り)")
        ),
        GrammarRule(
            GrammarPoint(
                id = "g_n3_wake_dewa_nai",
                pattern = "～わけではない",
                level = JlptLevel.N3,
                titleRu = "Вовсе не значит, что... / не то чтобы...",
                formula = "Простая форма + わけではない / わけではありません",
                explanationRu = "Мягкое отрицание вывода, который мог бы сделать собеседник.",
                exampleJp = "甘いものが嫌いなわけではないが、普段はあまり食べない。",
                exampleRu = "Не то чтобы я не любил сладкое, но обычно ем его редко."
            ),
            Regex("わけで(は)?(ない|ありません|はなく)")
        ),
        GrammarRule(
            GrammarPoint(
                id = "g_n3_ni_yotte",
                pattern = "～によって / ～による",
                level = JlptLevel.N3,
                titleRu = "В зависимости от / благодаря / посредством",
                formula = "Существительное + によって / による",
                explanationRu = "Указывает на причину, способ, автора или зависимость от обстоятельств («у каждого по-разному»).",
                exampleJp = "文化は地域によって大きく異なる。",
                exampleRu = "Культура сильно различается в зависимости от региона."
            ),
            Regex("によ(って|り|る|っては)")
        ),
        GrammarRule(
            GrammarPoint(
                id = "g_n3_ni_taishite",
                pattern = "～に対して",
                level = JlptLevel.N3,
                titleRu = "По отношению к / напротив, в то время как",
                formula = "Существительное + に対して / に対する",
                explanationRu = "Обозначает объект отношения/действия либо контраст между двумя явлениями.",
                exampleJp = "相手の意見に対して自分の考えを述べた。",
                exampleRu = "Я высказал своё мнение по поводу точки зрения собеседника."
            ),
            Regex("に対(して|する|しても)")
        ),
        GrammarRule(
            GrammarPoint(
                id = "g_n3_uchi_ni",
                pattern = "～うちに",
                level = JlptLevel.N3,
                titleRu = "Пока (не изменилось состояние) / в процессе",
                formula = "Глаг. (словарн./ている/ない) + うちに",
                explanationRu = "Либо «пока не поздно / пока длится условие», либо «незаметно в процессе этого действия произошло изменение».",
                exampleJp = "冷めないうちに召し上がってください。",
                exampleRu = "Пожалуйста, поешьте, пока не остыло."
            ),
            Regex("うちに")
        ),
        GrammarRule(
            GrammarPoint(
                id = "g_n4_te_itadakemasenka",
                pattern = "～ていただけませんか",
                level = JlptLevel.N4,
                titleRu = "Очень вежливая просьба («Не могли бы вы...?»)",
                formula = "Глагол (て/で-форма) + いただけませんか",
                explanationRu = "Вежливая форма просьбы, обращённая к незнакомому человеку, старшему или коллеге. Буквально: «Не мог бы я получить от вас это действие?».",
                exampleJp = "すみません。ちょっと写真を撮っていただけませんか。",
                exampleRu = "Извините, не могли бы вы сделать фотографию?"
            ),
            Regex("いただけませんか")
        ),
        GrammarRule(
            GrammarPoint(
                id = "g_n4_nakerebanaranai",
                pattern = "～なければならない",
                level = JlptLevel.N4,
                titleRu = "Необходимость («нужно, обязан»)",
                formula = "Глагол (ない-основа) + なければならない / なりません",
                explanationRu = "Выражает обязанность или строгую необходимость.",
                exampleJp = "ゴミを分別しなければなりません。",
                exampleRu = "Мусор необходимо сортировать."
            ),
            Regex("なければなら(ない|ません)")
        ),
        GrammarRule(
            GrammarPoint(
                id = "g_n5_tehaikenai",
                pattern = "～てはいけない",
                level = JlptLevel.N5,
                titleRu = "Запрет («нельзя делать»)",
                formula = "Глагол (て/で-форма) + はいけない / はいけません",
                explanationRu = "Выражает правило или строгий запрет.",
                exampleJp = "図書館で大きい声で話してはいけません。",
                exampleRu = "В библиотеке нельзя громко разговаривать."
            ),
            Regex("[てで]はいけ(ない|ません)")
        ),
        GrammarRule(
            GrammarPoint(
                id = "g_n4_koto_ga_dekiru",
                pattern = "～ことができる",
                level = JlptLevel.N4,
                titleRu = "Возможность («мочь, уметь»)",
                formula = "Глагол (словарная форма) + ことができます / できました",
                explanationRu = "Описывает способность или объективную возможность выполнить действие.",
                exampleJp = "安心して働くことができました。",
                exampleRu = "Я смог спокойно работать."
            ),
            Regex("ことができ(る|ます|ました|た|ない|ません)")
        ),
        GrammarRule(
            GrammarPoint(
                id = "g_n4_ta_koto_ga_aru",
                pattern = "～たことがある",
                level = JlptLevel.N4,
                titleRu = "Прошлый опыт («бывало, приходилось»)",
                formula = "Глагол (た-форма) + ことがあります / なかった",
                explanationRu = "Говорит о наличии или отсутствии жизненного опыта в прошлом.",
                exampleJp = "インスタントしか飲んだことがなかったのです。",
                exampleRu = "До этого я пил только растворимый вариант."
            ),
            Regex("[ただ]ことが(ある|あり|ない|なかっ)")
        ),
        GrammarRule(
            GrammarPoint(
                id = "g_n4_younisuru",
                pattern = "～ようにする",
                level = JlptLevel.N4,
                titleRu = "Стараться выработать привычку",
                formula = "Глагол (словарная / ない-форма) + ようにする",
                explanationRu = "Описывает сознательное усилие приучить себя к чему-либо или следовать правилу.",
                exampleJp = "毎晩ストレッチをするようにしています。",
                exampleRu = "Я стараюсь каждый вечер делать растяжку."
            ),
            Regex("ようにし(て|ます|た|よう)")
        ),
        GrammarRule(
            GrammarPoint(
                id = "g_n4_youninaru",
                pattern = "～ようになる",
                level = JlptLevel.N4,
                titleRu = "Изменение состояния («стал уметь / начал»)",
                formula = "Глагол (словарная / потенц.) + ようになる",
                explanationRu = "Показывает постепенное изменение, когда раньше что-то не получалось, а теперь стало возможным.",
                exampleJp = "少しずつ読めるようになりました。",
                exampleRu = "Я постепенно научился читать."
            ),
            Regex("ようになり(ます|ました|った)")
        ),
        GrammarRule(
            GrammarPoint(
                id = "g_n4_kotonisuru_naru",
                pattern = "～ことにする / ～ことになる",
                level = JlptLevel.N4,
                titleRu = "Принять решение / так сложилось",
                formula = "Глагол (словарная) + ことにする (сам решил) / ことになる (так вышло)",
                explanationRu = "ことにする выражает личное решение говорящего; ことになる — решение обстоятельств или других людей.",
                exampleJp = "本屋でアルバイトを始めることにしました。",
                exampleRu = "Я решил начать подрабатывать в книжном магазине."
            ),
            Regex("ことに(し|なり)(ます|ました|て|た)")
        ),
        GrammarRule(
            GrammarPoint(
                id = "g_n4_kamoshirenai",
                pattern = "～かもしれない",
                level = JlptLevel.N4,
                titleRu = "Вероятность («возможно, может быть»)",
                formula = "Простая форма + かもしれない / かもしれません",
                explanationRu = "Выражает предположение с долей неуверенности.",
                exampleJp = "もう直せないかもしれないと思っていました。",
                exampleRu = "Я думал, что, возможно, уже не починить."
            ),
            Regex("かもしれ(ない|ません)")
        ),
        GrammarRule(
            GrammarPoint(
                id = "g_n4_hougaii",
                pattern = "～ほうがいい",
                level = JlptLevel.N4,
                titleRu = "Совет («лучше сделать / не делать»)",
                formula = "Глагол (た-форма / ない-форма) + ほうがいいです",
                explanationRu = "Используется для мягкого совета или рекомендации.",
                exampleJp = "お湯を沸騰させないほうがいいそうです。",
                exampleRu = "Говорят, воду лучше не доводить до кипения."
            ),
            Regex("ほうがいい")
        ),
        GrammarRule(
            GrammarPoint(
                id = "g_n4_temiru",
                pattern = "～てみる",
                level = JlptLevel.N4,
                titleRu = "Попробовать сделать что-либо",
                formula = "Глагол (て/で-форма) + みる / みます / みた",
                explanationRu = "Выражает попытку сделать что-то, чтобы посмотреть на результат или получить новый опыт.",
                exampleJp = "日本語で日記を書いてみます。",
                exampleRu = "Попробую вести дневник на японском."
            ),
            Regex("[てで]み(る|ます|ました|て|た|よう)")
        ),
        GrammarRule(
            GrammarPoint(
                id = "g_n4_teshimau",
                pattern = "～てしまう",
                level = JlptLevel.N4,
                titleRu = "Завершённость действия / сожаление",
                formula = "Глагол (て/で-форма) + しまう / しまいます / しまった",
                explanationRu = "Обозначает полностью завершённое действие либо оттенок сожаления, случайности, ошибки.",
                exampleJp = "うっかり忘れてしまいました。",
                exampleRu = "Я случайно забыл."
            ),
            Regex("[てで]しま(う|います|いました|った|って)")
        ),
        GrammarRule(
            GrammarPoint(
                id = "g_n4_teoku",
                pattern = "～ておく",
                level = JlptLevel.N4,
                titleRu = "Сделать заранее (впрок)",
                formula = "Глагол (て/で-форма) + おく / おきます / おいた",
                explanationRu = "Указывает, что действие выполняется заранее как подготовка к будущему.",
                exampleJp = "写真を撮っておくようにしています。",
                exampleRu = "Я стараюсь заранее сделать фото."
            ),
            Regex("[てで]お(く|きます|いた|いて|ける)")
        ),
        GrammarRule(
            GrammarPoint(
                id = "g_n4_tekureru_morau",
                pattern = "～てくれる / ～てもらう",
                level = JlptLevel.N4,
                titleRu = "Действие для меня / получение помощи",
                formula = "Глагол (て/で-форма) + くれる (для меня) / もらう (получил помощь)",
                explanationRu = "てくれる подчёркивает доброту другого человека к говорящему; てもらう — что говорящий попросил или получил помощь.",
                exampleJp = "先輩が丁寧に教えてくれました。",
                exampleRu = "Старший коллега вежливо объяснил мне."
            ),
            Regex("[てで](くれ|もら)(る|ます|ました|た|い|え)")
        ),
        GrammarRule(
            GrammarPoint(
                id = "g_n5_tekudasai",
                pattern = "～てください",
                level = JlptLevel.N5,
                titleRu = "Вежливая просьба («пожалуйста, сделайте»)",
                formula = "Глагол (て/で-форма) + ください",
                explanationRu = "Используется для вежливой просьбы или инструкции выполнить действие.",
                exampleJp = "この道をまっすぐ行ってください。",
                exampleRu = "Пожалуйста, идите прямо по этой дороге."
            ),
            Regex("[てで]ください")
        ),
        GrammarRule(
            GrammarPoint(
                id = "g_n5_te_kara",
                pattern = "～てから",
                level = JlptLevel.N5,
                titleRu = "Последовательность («после того как...»)",
                formula = "Глагол (て/で-форма) + から、...",
                explanationRu = "Показывает, что второе действие происходит сразу после завершения первого.",
                exampleJp = "荷物を預けてから、朝ご飯を食べました。",
                exampleRu = "Сдав багаж, я позавтракал."
            ),
            Regex("[てで]から")
        ),
        GrammarRule(
            GrammarPoint(
                id = "g_n5_teiru",
                pattern = "～ている / ～ています",
                level = JlptLevel.N5,
                titleRu = "Длительное действие или текущее состояние",
                formula = "Глагол (て/で-форма) + いる / います / いました",
                explanationRu = "Обозначает действие, происходящее прямо сейчас, регулярную привычку или сохраняющееся состояние.",
                exampleJp = "自動販売機で温かいスープも売っています。",
                exampleRu = "В автоматах также продаётся горячий суп."
            ),
            Regex("[てで]い(る|ます|ました|た|ない)")
        ),
        GrammarRule(
            GrammarPoint(
                id = "g_n4_tsumori",
                pattern = "～つもり",
                level = JlptLevel.N4,
                titleRu = "Намерение, план",
                formula = "Глагол (словарная / ない-форма) + つもりです",
                explanationRu = "Выражает твёрдое намерение или заранее обдуманный план говорящего.",
                exampleJp = "二十分ほど歩くつもりです。",
                exampleRu = "Я планирую пройти пешком около 20 минут."
            ),
            Regex("つもり(です|だ)")
        ),
        GrammarRule(
            GrammarPoint(
                id = "g_n4_souda",
                pattern = "～そうだ (внешний вид / слухи)",
                level = JlptLevel.N4,
                titleRu = "Кажется / говорят, что...",
                formula = "Основа прил./глаг. + そうだ (вид) | Простая форма + そうです (слухи)",
                explanationRu = "С основой означает «выглядит так, будто...»; после полной формы передаёт информацию с чужих слов («говорят, что...»).",
                exampleJp = "通勤がとても便利になりそうです。",
                exampleRu = "Поездки на работу, похоже, станут очень удобными."
            ),
            Regex("(?<![、。「\\s])そう(です|だ|な|に)")
        ),
        GrammarRule(
            GrammarPoint(
                id = "g_n4_youda_mitaida",
                pattern = "～ようだ / ～みたいだ",
                level = JlptLevel.N4,
                titleRu = "Похоже, что... / словно",
                formula = "Простая форма + ようだ / ようです / みたいだ",
                explanationRu = "Выражает предположение на основе собственных наблюдений или сравнение.",
                exampleJp = "用紙が切れているみたいです。",
                exampleRu = "Похоже, закончилась бумага."
            ),
            Regex("(?<!おは|にち|げつ|か|すい|もく|きん|ど|し)よう(です|だ|な(?!り|し)|に(?!し|な))|みたい(です|だ)")
        ),
        GrammarRule(
            GrammarPoint(
                id = "g_n4_yasui_nikui",
                pattern = "～やすい / ～にくい",
                level = JlptLevel.N4,
                titleRu = "Легко / трудно делать что-либо",
                formula = "Глагол (ます-основа) + やすい / にくい",
                explanationRu = "Описывает свойство предмета или действия: насколько легко или трудно его совершить.",
                exampleJp = "新宿駅は広くて迷いやすいです。",
                exampleRu = "Станция Синдзюку большая, поэтому там легко заблудиться."
            ),
            Regex("(やすい|やすく|にくい|にくく)")
        ),
        GrammarRule(
            GrammarPoint(
                id = "g_n4_sugiru",
                pattern = "～すぎる",
                level = JlptLevel.N4,
                titleRu = "Чрезмерность («слишком...»)",
                formula = "Глагол (ます-основа) / Прил. (основа) + すぎる",
                explanationRu = "Показывает, что действие или качество превышает норму.",
                exampleJp = "聞き取ろうとしすぎないことが大切です。",
                exampleRu = "Важно не пытаться расслышать абсолютно всё."
            ),
            Regex("すぎ(る|ます|た|て|ない)")
        ),
        GrammarRule(
            GrammarPoint(
                id = "g_n4_nagara",
                pattern = "～ながら",
                level = JlptLevel.N4,
                titleRu = "Одновременное действие («во время / -я»)",
                formula = "Глагол (ます-основа) + ながら、...",
                explanationRu = "Показывает, что один человек совершает два действия одновременно.",
                exampleJp = "雨粒を眺めながらアップルパイを食べました。",
                exampleRu = "Я ел яблочный пирог, любуясь каплями дождя."
            ),
            Regex("ながら")
        ),
        GrammarRule(
            GrammarPoint(
                id = "g_n4_node",
                pattern = "～ので",
                level = JlptLevel.N4,
                titleRu = "Мягкая причина («так как, поскольку»)",
                formula = "Простая форма (сущ./な-прил. + な) + ので",
                explanationRu = "Указывает на объективную причину; звучит мягче и вежливее, чем から.",
                exampleJp = "駅から歩いて五分なので、便利です。",
                exampleRu = "Поскольку от станции 5 минут пешком, это удобно."
            ),
            Regex("ので")
        ),
        GrammarRule(
            GrammarPoint(
                id = "g_n4_noni",
                pattern = "～のに",
                level = JlptLevel.N4,
                titleRu = "Уступка («хотя, несмотря на»)",
                formula = "Простая форма (сущ./な-прил. + な) + のに",
                explanationRu = "Выражает контраст, когда результат не соответствует ожиданиям.",
                exampleJp = "初対面なのに一緒に連弾をしていました。",
                exampleRu = "Хотя они встретились впервые, они играли в четыре руки."
            ),
            Regex("のに")
        ),
        GrammarRule(
            GrammarPoint(
                id = "g_n4_tara_ba_nara",
                pattern = "～たら / ～ば / ～なら (условие)",
                level = JlptLevel.N4,
                titleRu = "Условное наклонение («если / когда»)",
                formula = "Глагол (た-форма + ら / условная основа + ば) | Сущ./Глаг. + なら",
                explanationRu = "Выражает условие или момент наступления события («если сделаешь...», «когда открыл...»).",
                exampleJp = "言われた通りに作ったら、おいしくなりました。",
                exampleRu = "Когда я приготовил как было сказано, получилось вкусно."
            ),
            Regex("(たら、|(?<!なけ)れば|(?<!なければ)なら(?!ない|ません))")
        ),
        GrammarRule(
            GrammarPoint(
                id = "g_n4_temo",
                pattern = "～ても (уступка «даже если»)",
                level = JlptLevel.N4,
                titleRu = "Уступка («даже если...»)",
                formula = "Глагол (て/で-форма) + も",
                explanationRu = "Показывает, что результат не изменится, даже если выполнится указанное условие.",
                exampleJp = "少し間違えても大丈夫です。",
                exampleRu = "Даже если немного ошибёшься — ничего страшного."
            ),
            Regex("(?<![とでそこど])[てで]も")
        ),
        GrammarRule(
            GrammarPoint(
                id = "g_n4_tameni_okage_sei",
                pattern = "～ために / ～おかげで / ～せいで",
                level = JlptLevel.N4,
                titleRu = "Цель или причина («ради / благодаря / из-за»)",
                formula = "Глаг./Сущ.の + ために (ради) | おかげで (благодаря +) | せいで (из-за -)",
                explanationRu = "ために обозначает цель; おかげで — благодарность за хороший результат; せいで — причину неприятности.",
                exampleJp = "毎日読み続けたおかげで、わかる文が増えました。",
                exampleRu = "Благодаря тому, что продолжал читать каждый день, стало больше понятных фраз."
            ),
            Regex("(ために|ため、|おかげで|せいで|せいか)")
        ),
        GrammarRule(
            GrammarPoint(
                id = "g_n4_hazu",
                pattern = "～はず",
                level = JlptLevel.N4,
                titleRu = "Уверенное ожидание («должно быть»)",
                formula = "Простая форма + はずです / はずなのに",
                explanationRu = "Выражает логическую уверенность говорящего на основе имеющихся фактов.",
                exampleJp = "きっといい変化が感じられるはずです。",
                exampleRu = "Наверняка должны почувствоваться хорошие перемены."
            ),
            Regex("はず")
        ),
        GrammarRule(
            GrammarPoint(
                id = "g_n4_bakari",
                pattern = "～ばかり",
                level = JlptLevel.N4,
                titleRu = "Только что / сплошь одно и то же",
                formula = "Глагол (た-форма) + ばかりです | Сущ. + ばかり",
                explanationRu = "После た-формы означает «только что сделал»; после существительного — «только лишь...».",
                exampleJp = "まだ始めたばかりです。",
                exampleRu = "Я только недавно начал."
            ),
            Regex("ばかり")
        ),
        GrammarRule(
            GrammarPoint(
                id = "g_n5_tari",
                pattern = "～たり～たりする",
                level = JlptLevel.N5,
                titleRu = "Перечисление действий («и то, и другое»)",
                formula = "Глагол (た-форма) + り、... + りする / します",
                explanationRu = "Перечисляет несколько примеров действий из числа многих.",
                exampleJp = "お弁当を食べたり、お茶を飲んだりします。",
                exampleRu = "Едят бэнто, пьют чай и т.п."
            ),
            Regex("[ただ]り(し|する|します|した)")
        ),
        GrammarRule(
            GrammarPoint(
                id = "g_n5_tai",
                pattern = "～たい",
                level = JlptLevel.N5,
                titleRu = "Желание («хочу сделать»)",
                formula = "Глагол (ます-основа) + たいです / たかった",
                explanationRu = "Выражает собственное желание говорящего совершить действие.",
                exampleJp = "また京都へ行きたいです。",
                exampleRu = "Я хочу снова поехать в Киото."
            ),
            Regex("(?<!み|だ|い|し|き)たい(です|と|か)|(?<!み)たかった")
        ),
        GrammarRule(
            GrammarPoint(
                id = "g_n5_mae_ato",
                pattern = "～前に / ～後で",
                level = JlptLevel.N5,
                titleRu = "Время действия («до / после»)",
                formula = "Глагол (словарная) + 前に | Глагол (た-форма) / Сущ. + の + 後で",
                explanationRu = "Указывает, происходит ли главное действие до или после другого события.",
                exampleJp = "寝る前にストレッチをします。",
                exampleRu = "Перед сном я делаю растяжку."
            ),
            Regex("(前に|後で)")
        ),
        GrammarRule(
            GrammarPoint(
                id = "g_n5_kara_reason",
                pattern = "～から (причина)",
                level = JlptLevel.N5,
                titleRu = "Причина («потому что...»)",
                formula = "Причина + から、Результат",
                explanationRu = "Указывает на причину или основание. В конце предложения часто ставится ～からです.",
                exampleJp = "図書館は静かですから、本が読みやすいです。",
                exampleRu = "Поскольку в библиотеке тихо, читать книги легко."
            ),
            Regex("(ですから|ますから|(?<![てで])からです)")
        ),
        GrammarRule(
            GrammarPoint(
                id = "g_n5_mashou_masenka",
                pattern = "～ましょう / ～ませんか",
                level = JlptLevel.N5,
                titleRu = "Приглашение к совместному действию",
                formula = "Глагол (ます-основа) + ましょう (давайте) / ませんか (не хотите ли?)",
                explanationRu = "～ませんか — мягкое приглашение, ～ましょう — предложение сделать что-то вместе.",
                exampleJp = "一緒に映画を見に行きませんか。",
                exampleRu = "Не сходить ли нам вместе в кино?"
            ),
            Regex("(?<!いただけ|くれ|あり|でき|わかり)(ましょう|ませんか)")
        ),
        GrammarRule(
            GrammarPoint(
                id = "g_n5_kedo_ga",
                pattern = "～が / ～けれど (противопоставление)",
                level = JlptLevel.N5,
                titleRu = "Сопоставление («но, хотя»)",
                formula = "Часть 1 + が / けれど（も）、Часть 2",
                explanationRu = "Соединяет две части предложения с оттенком контраста или мягкого перехода к новой теме.",
                exampleJp = "少し難しいですが、とても面白いです。",
                exampleRu = "Немного сложно, но очень интересно."
            ),
            Regex("(ですが、|ますが、|ましたが、|けれど|けど)")
        ),
        GrammarRule(
            GrammarPoint(
                id = "g_n5_ga_arimasu_imasu",
                pattern = "～があります / ～います",
                level = JlptLevel.N5,
                titleRu = "Наличие предметов и живых существ",
                formula = "Место + に + Предмет + が あります / Существо + が います",
                explanationRu = "あります используется для неодушевлённых предметов и событий, а います — для людей и животных.",
                exampleJp = "公園に小さい白い猫がいました。",
                exampleRu = "В парке была маленькая белая кошка."
            ),
            Regex("が(あり|いま)(す|した|せん)")
        ),
        GrammarRule(
            GrammarPoint(
                id = "g_n5_suki_jouzu",
                pattern = "～が好き / 上手 / 苦手",
                level = JlptLevel.N5,
                titleRu = "Нравится / умею / плохо удаётся",
                formula = "Объект + が + 好きです / 上手です / 苦手です",
                explanationRu = "Выражает отношение к чему-либо или оценку своих навыков. Перед таким предикативом всегда ставится частица が.",
                exampleJp = "私は秋が一番好きです。",
                exampleRu = "Больше всего мне нравится осень."
            ),
            Regex("が[^。、]*(好き|大好き|上手|苦手)")
        ),
        GrammarRule(
            GrammarPoint(
                id = "g_n5_desu_masu",
                pattern = "～です / ～ます",
                level = JlptLevel.N5,
                titleRu = "Вежливая форма речи (наст./буд. и прош. время)",
                formula = "Сущ./Прил. + です (でした) | Глагол (ます-основа) + ます (ました)",
                explanationRu = "Базовая нейтрально-вежливая форма японского предложения. Используется в повседневном общении.",
                exampleJp = "私は毎日日本語を勉強します。",
                exampleRu = "Я каждый день изучаю японский язык."
            ),
            Regex("(です|でした|ます(?!か)|ました|ません(?!か))")
        )
    )

    fun findGrammarInMaterial(material: ReadingMaterial): List<GrammarPoint> {
        val fullText = material.plainText()
        return rules
            .filter { it.regex.containsMatchIn(fullText) }
            .map { it.point }
    }

    /**
     * Finds the most specific grammar rule matching a single segment's text.
     */
    fun findGrammarForSegment(segmentText: String): GrammarPoint? =
        rules.firstOrNull { it.regex.containsMatchIn(segmentText) }?.point
}
