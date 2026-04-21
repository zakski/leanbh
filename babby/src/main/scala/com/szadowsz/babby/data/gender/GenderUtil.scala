package com.szadowsz.babby.data.gender

/**
  * Created on 28/10/2016.
  */
object GenderUtil {


  def forName(gen: String): Gender = {
    gen.toLowerCase() match {
      case x if Gender.BOY.synonyms.contains(x) => Gender.BOY
      case y if Gender.GIRL.synonyms.contains(y) => Gender.GIRL
      case _ => Gender.BOTH
    }
  }

}
