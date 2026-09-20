package com.marko.turista
import org.json.*
/** Scores are only the provider's scores, never generated confidence percentages. */
object RecognitionResults {
 data class Reference(val url:String,val author:String,val license:String)
 data class Candidate(val name:String,val scientific:String,val description:String,val score:Double?=null,val references:List<Reference> = emptyList())
 fun parse(answer:String):List<Candidate>{
  val json=JSONObject(answer.substringAfter('\n').trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim())
  val plant=answer.startsWith("PLANTNET\n")
  val items=json.optJSONArray(if(plant)"results" else "candidates")?:return emptyList()
  return buildList {for(i in 0 until minOf(items.length(),5)){
   val item=items.optJSONObject(i)?:continue
   if(plant){
    val species=item.optJSONObject("species")?:continue
    val sci=species.optString("scientificNameWithoutAuthor",species.optString("scientificName")).take(200)
    if(sci.isBlank())continue
    val common=species.optJSONArray("commonNames")?.optString(0).orEmpty()
    val family=species.optJSONObject("family")?.optString("scientificNameWithoutAuthor").orEmpty()
    val score=item.optDouble("score",Double.NaN).takeIf{it.isFinite() && it in 0.0..1.0}
    val refs=buildList{
     val images=item.optJSONArray("images")?:JSONArray()
     for(j in 0 until minOf(images.length(),3)){
      val img=images.optJSONObject(j)?:continue
      val url=img.optJSONObject("url")?.optString("s").orEmpty()
      val author=img.optString("author");val license=img.optString("license")
      val knownLicense=license.uppercase().replace(" ","-").let{it.startsWith("CC-BY") || it.startsWith("CC0")}
      if(url.startsWith("https://") && author.isNotBlank() && knownLicense)add(Reference(url,author,license))
     }
    }
    add(Candidate(common.ifBlank{sci},sci,if(family.isBlank())"Odhad Pl@ntNet" else "Čeľaď: $family",score,refs))
   }else{
    val name=item.optString("name").take(200)
    if(name.isNotBlank())add(Candidate(name,item.optString("scientific").take(200),item.optString("description").take(4000)))
   }
  }}
 }
 fun plantOrgan(label:String)=when(label){"List"->"leaf";"Kvet"->"flower";"Plod / semeno"->"fruit";"Kôra"->"bark";else->"auto"}
}
