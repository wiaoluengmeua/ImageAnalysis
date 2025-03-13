//0.1.2 0.2.0

getTMACoreList().each{
  it.setMissing(false)
}
fireHierarchyUpdate()
println("done")
