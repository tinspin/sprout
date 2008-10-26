function myKeyPressed(e) {
  e = e || window.event;
  var unicode = e.charCode ? e.charCode : e.keyCode ? e.keyCode : 0;
  if(unicode == 13) {
    document.forms[0].submit();
    return false;
  }
}