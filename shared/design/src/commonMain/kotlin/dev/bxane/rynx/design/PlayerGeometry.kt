package dev.bxane.rynx.design

const val NavigationDockHeight = 82f

data class PlayerArtworkGeometry(val left:Float,val top:Float,val width:Float,val height:Float)
data class PlayerGeometry(
    val left:Float,val top:Float,val width:Float,val height:Float,val radius:Float,
    val artwork:PlayerArtworkGeometry,val navigationReveal:Float,val navigationInteractive:Boolean,val landscape:Boolean=false,
)

fun progressBetween(progress:Float,start:Float,end:Float):Float {
    val t=((progress-start)/(end-start)).coerceIn(0f,1f)
    return t*t*(3f-2f*t)
}

/** All dimensions are density-independent; the caller converts them once at its boundary. */
fun playerGeometry(width:Float,height:Float,progress:Float):PlayerGeometry {
    val p=if(progress.isFinite())progress.coerceIn(0f,1f) else 1f
    fun mix(a:Float,b:Float)=a+(b-a)*p
    val expandedHeight=(height-NavigationDockHeight).coerceAtLeast(62f)
    val landscape=width>=600f && height<600f && width>height
    val reveal=progressBetween(p,.42f,1f)
    return PlayerGeometry(
        left=mix(12f,0f),top=mix(height-74f,0f),width=mix(width-24f,width),height=mix(62f,expandedHeight),
        radius=mix(27f,28f),
        artwork=PlayerArtworkGeometry(mix(10f,0f),mix(11f,0f),mix(40f,if(landscape)width*.43f else width),mix(40f,if(landscape)expandedHeight else expandedHeight*.69f)),
        navigationReveal=reveal,navigationInteractive=reveal>=.96f,landscape=landscape,
    )
}
