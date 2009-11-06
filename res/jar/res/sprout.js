function myKeyPressed(e) {
  e = e || window.event;
  var unicode = e.charCode ? e.charCode : e.keyCode ? e.keyCode : 0;
  if(unicode == 13) {
    document.forms[0].submit();
    return false;
  }
}
function remind() {
  if(document.getElementById('mail').value != '') {
    document.getElementById('login').action = 'remind';
    document.getElementById('login').submit();
  }
  return false;
}									
function columnize(content, parent, push) {
  var max = 0, art = 0;
  var div = content.getElementsByTagName('div');
  
  if(div.length == 0) {
    return;
  }
  
  var col = new Array(Math.floor(parent.offsetWidth / div[0].offsetWidth));

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
      div[j].style.visibility = 'visible';
      col[mod] = col[mod] + height;
      
      if(col[mod] > max) {
        max = col[mod];
      }
      
      art++;
      
      makeDraggable(div[j]);
    }
  }
  
  content.style.height = max + 100 + 'px';
}
document.onmousemove = mouseMove;
document.onmouseup   = mouseUp;
var dragObject  = null;
var mouseOffset = null;
var topIndex    = 1;
function getMouseOffset(target, ev) {
  ev = ev || window.event;

  var docPos    = getPosition(target);
  var mousePos  = mouseCoords(ev);
  return {x:mousePos.x - docPos.x, y:mousePos.y - docPos.y};
}
function getPosition(e) {
  var left = 0;
  var top  = 0;

  while (e.offsetParent) {
    left += e.offsetLeft;
    top  += e.offsetTop;
    e     = e.offsetParent;
  }

  left += e.offsetLeft;
  top  += e.offsetTop;

  return {x:left, y:top};
}
function mouseMove(ev) {
  ev           = ev || window.event;
  var mousePos = mouseCoords(ev);

  if(dragObject) {
    //dragObject.style.position = 'absolute';
    dragObject.style.top      = mousePos.y - mouseOffset.y - dragObject.style.marginTop.replace('px', '') + 'px';
    dragObject.style.left     = mousePos.x - mouseOffset.x - dragObject.style.marginLeft.replace('px', '') + 'px';

    return false;
  }
}
function mouseCoords(ev) {
  if(ev.pageX || ev.pageY) {
    return {x:ev.pageX, y:ev.pageY};
  }
  if(document.body) {
    return {
      x:ev.clientX + document.body.scrollLeft - document.body.clientLeft,
      y:ev.clientY + document.body.scrollTop  - document.body.clientTop
    };
  }
  return {0, 0};
}
function mouseUp() {
  dragObject = null;
}
function makeDraggable(item) {
  if(!item) return;
  item.onmousedown = function(ev) {
    dragObject              = this;
    dragObject.style.zIndex = topIndex++;
    mouseOffset             = getMouseOffset(this, ev);
    return false;
  }
}