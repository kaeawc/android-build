package dev.jasonpearson.experimentation

interface Treatment {
  val id: String
  val label: String

  fun getControl(): Treatment
}
