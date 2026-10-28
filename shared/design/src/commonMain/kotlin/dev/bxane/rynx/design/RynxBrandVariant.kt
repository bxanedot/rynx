package dev.bxane.rynx.design

/** Palettes and authored liquid finishes from the owner-supplied Rynx mark archives. */
enum class RynxBrandVariant(
    val label: String,
    val topStartArgb: Long = 0x00000000L,
    val topEndArgb: Long = 0x00000000L,
    val bottomStartArgb: Long = 0x00000000L,
    val bottomEndArgb: Long = 0x00000000L,
    val liquidTextureIndex: Int? = null,
) {
    SilverWhite("Silver", 0xFFFFFFFFL, 0xFFBFC7D3L, 0xFFE9EEF6L, 0xFF8B97A6L),
    GraphiteChrome("Graphite", 0xFF737C8EL, 0xFF242A36L, 0xFF3C4658L, 0xFF131923L),
    ElectricBlue("Electric blue", 0xFF00E5FFL, 0xFF2458FDL, 0xFF22DDFCL, 0xFF5430EFL),
    PurpleMagenta("Purple magenta", 0xFF7C35F2L, 0xFFEE21DEL, 0xFFCA35FAL, 0xFFF40C8BL),
    AmberOrange("Amber orange", 0xFFFFE154L, 0xFFFF7E1BL, 0xFFFFAB25L, 0xFFF83B38L),
    RubyRed("Ruby red", 0xFFFF466BL, 0xFFD60035L, 0xFFFF245AL, 0xFF850025L),
    EmeraldTeal("Emerald teal", 0xFF4CFFABL, 0xFF00B9ADL, 0xFF00DFA8L, 0xFF187BE9L),
    IridescentPastel("Iridescent pastel", 0xFFC6ABFFL, 0xFFFA8BC9L, 0xFFFBAFA1L, 0xFFFFA97BL),
    FrostedIceBlue("Frosted ice", 0xFFF6FFFFL, 0xFF69D9F4L, 0xFF9AE9FFL, 0xFF0A86E8L),
    ChampagneGold("Champagne gold", 0xFFFFF2ABL, 0xFFD89A3AL, 0xFFFFDF74L, 0xFFB9762DL),

    LiquidFinish01("Liquid finish 01", liquidTextureIndex = 1),
    LiquidFinish02("Liquid finish 02", liquidTextureIndex = 2),
    LiquidFinish03("Liquid finish 03", liquidTextureIndex = 3),
    LiquidFinish04("Liquid finish 04", liquidTextureIndex = 4),
    LiquidFinish05("Liquid finish 05", liquidTextureIndex = 5),
    LiquidFinish06("Liquid finish 06", liquidTextureIndex = 6),
    LiquidFinish07("Liquid finish 07", liquidTextureIndex = 7),
    LiquidFinish08("Liquid finish 08", liquidTextureIndex = 8),
    LiquidFinish09("Liquid finish 09", liquidTextureIndex = 9),
    LiquidFinish10("Liquid finish 10", liquidTextureIndex = 10),
}
