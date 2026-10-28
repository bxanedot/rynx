package dev.bxane.rynx.ui.material

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.bxane.rynx.design.*
import dev.bxane.rynx.ui.Glyph
import dev.bxane.rynx.ui.RynxGlyph

@Composable internal fun RynxSegments(options:List<Pair<String,String>>,selected:Int,onSelect:(Int)->Unit,modifier:Modifier=Modifier) {
    val reduced=LocalRynxReducedMotion.current
    val position by animateFloatAsState(selected.toFloat(),if(reduced)snap() else spring(stiffness=RynxMotionTokens.SelectionStiffness,dampingRatio=RynxMotionTokens.SelectionDamping))
    val x=if(LocalRynxReducedMotion.current)selected.toFloat() else position
    val dark=LocalRynxDarkSurface.current
    RynxSurface(modifier.fillMaxWidth().height(50.dp),20.dp) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(3.dp)) {
            val cell=maxWidth/options.size
            Box(Modifier.offset(x=cell*x).width(cell).fillMaxHeight().rynxMaterial(17.dp,selected=true,amount=.8f))
            Row(Modifier.fillMaxSize()) {
                options.forEachIndexed {index,(text,label)->
                    val interaction=remember {MutableInteractionSource()}
                    Box(Modifier.weight(1f).fillMaxHeight().rynxPressFeedback(interaction)
                        .clickable(role=Role.RadioButton,interactionSource=interaction,indication=null){onSelect(index)}
                        .semantics {contentDescription=label;this.selected=index==selected},contentAlignment=Alignment.Center) {
                        BasicText(text,style=TextStyle(color=if(dark)Color.White else Color(0xFF22242C),fontSize=12.sp,fontWeight=if(index==selected)FontWeight.SemiBold else FontWeight.Normal))
                    }
                }
            }
        }
    }
}

@Composable internal fun RynxSearchInput(value:String,onChange:(String)->Unit,onSearch:()->Unit,modifier:Modifier=Modifier) {
    val interaction=remember {MutableInteractionSource()}
    val dark=LocalRynxAppearance.current.darkTheme || LocalRynxAppearance.current.material==RynxMaterialMode.Amoled
    val ink=if(dark)Color.White else Color(0xFF25262D)
    val keyboard=LocalSoftwareKeyboardController.current
    val depth=LocalRynxMaterialDepth.current
    Row(modifier.fillMaxWidth().heightIn(min=52.dp).rynxPressFeedback(interaction)
        .rynxMaterial(15.dp,interaction=interaction).padding(horizontal=12.dp,vertical=4.dp),verticalAlignment=Alignment.CenterVertically) {
        RynxGlyph(Glyph.Search,ink.copy(alpha=.5f))
        Box(Modifier.padding(start=10.dp).weight(1f)) {
            if(value.isEmpty())BasicText("Search your music",style=TextStyle(color=ink.copy(alpha=.5f),fontSize=15.sp))
            BasicTextField(value,onChange,Modifier.fillMaxWidth().semantics {contentDescription="Search your music"},
                singleLine=true,textStyle=TextStyle(color=ink,fontSize=15.sp),interactionSource=interaction,
                keyboardOptions=KeyboardOptions(imeAction=ImeAction.Search),
                keyboardActions=KeyboardActions(onSearch={keyboard?.hide();onSearch()}))
        }
        if(value.isNotEmpty())CompositionLocalProvider(LocalRynxMaterialDepth provides depth+1) {
            val clear=remember {MutableInteractionSource()}
            Box(Modifier.size(44.dp).rynxPressFeedback(clear).rynxMaterial(22.dp,interaction=clear,amount=.3f)
                .clickable(role=Role.Button,interactionSource=clear,indication=null){onChange("")}
                .semantics {contentDescription="Clear search"},contentAlignment=Alignment.Center) {
                RynxGlyph(Glyph.Close,ink.copy(alpha=.6f),Modifier.size(18.dp))
            }
        }
    }
}
