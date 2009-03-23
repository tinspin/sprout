function myKeyPressed(e) {
  e = e || window.event;
  var unicode = e.charCode ? e.charCode : e.keyCode ? e.keyCode : 0;
  if(unicode == 13) {
    document.forms[0].submit();
    return false;
  }
}
function columnize(content, push) {
  var max = 0, art = 0;
  var div = content.getElementsByTagName('div');
  
  if(div.length == 0) {
    return;
  }
  
  var col = new Array(Math.floor(content.offsetWidth / div[0].offsetWidth));

  for(i = 0; i < col.length; i++) {
    col[i] = 0;
    
    if(i == col.length - 1) {
      col[i] = push;
    }
  }
  
  for(j = 0; j < div.length; j++) {
    if(div[j].className == 'article') {
      var top = div[j].offsetTop;
      var height = div[j].offsetHeight;
      var mod = art % col.length;
      
      div[j].style.marginLeft = 225 * mod + 'px';
      div[j].style.marginTop = col[mod] + 10 * Math.floor(art / col.length) + 'px';
      col[mod] = col[mod] + height;
      
      if(col[mod] > max) {
        max = col[mod];
      }
      
      art++;
    }
  }
  
  content.style.height = max + 100 + 'px';
}
