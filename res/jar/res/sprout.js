function myKeyPressed(e) {
  e = e || window.event;
  var unicode = e.charCode ? e.charCode : e.keyCode ? e.keyCode : 0;
  if(unicode == 13) {
    document.forms[0].submit();
    return false;
  }
}
function myLoginKeyPressed(e) {
  e = e || window.event;
  var unicode = e.charCode ? e.charCode : e.keyCode ? e.keyCode : 0;
  if(unicode == 13) {
    ajaxLogin();
    return false;
  }
}
function changeFlag(country) {
  var flag = document.getElementById('flag');
  var code = country[country.selectedIndex].value.toLowerCase();
  if(code == '--') {
    flag.style.display = 'none';
  }
  else {
    flag.src = 'res/flag/' + code + '.png';
    flag.style.display = 'inline';
  }
}
function ajaxLogin() {
	_query('POST', '/login', 'ajax=true&mail=' + document.getElementById('mail').value + '&pass=' + document.getElementById('pass').value);
}
function ajaxPost(id) {
	_query('POST', '/comment', 'ajax=true&body=' + document.getElementById('body').value + '&id=' + id);
}
function remind() {
  if(document.getElementById('mail').value != '') {
    document.getElementById('login').action = 'remind';
    document.getElementById('login').submit();
  }
  return false;
}							
function columnize(push) {
  var content = document.getElementById("content");
  var parent = document.getElementById("page");

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
      
      div[j].style.left = 10 + 225 * mod + 'px';
      div[j].style.top = 10 + col[mod] + 10 * Math.floor(art / col.length) + 'px';
      div[j].style.visibility = 'visible';
      col[mod] = col[mod] + height;
      
      if(col[mod] > max) {
        max = col[mod];
      }
      
      art++;
      
      if(div[j])
      	new DragObject(div[j]);
      //makeDraggable(div[j]);
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
  return {
    x:ev.clientX + document.body.scrollLeft - document.body.clientLeft,
    y:ev.clientY + document.body.scrollTop  - document.body.clientTop
  };
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
var request;
function _query(method, url, data) {
  request = create();
  request.open(method, url, true);
  request.onreadystatechange = callback;
  request.send(data);
}
function _reply(data) {
  if(data.url) {
    self.location.href = data.url;
  }
  if(data.error) {
    document.getElementById('error').innerHTML = '<font color="red"><i>' + data.error + '</i></font>';
  }
  if(data.remind) {
    document.getElementById('remind').style.display = 'block';
  }
}
function callback() {
  try {
    if(request.readyState == 4) {
      if(request.status == 200) {
        //alert(request.responseText);
        _reply(eval('(' + request.responseText + ')'));
      }
      else if(request.status == 500) {
        alert(request.responseText);
      }
    }
  } catch (e) {
    alert(e);
  }
}
function create() {
  try { return new XMLHttpRequest(); } catch (e) {
    try { return new ActiveXObject('microsoft.xmlhttp'); } catch (e) {
      try { return new ActiveXObject('msxml2.xmlhttp'); } catch (e) {}
    }
  }
  return null;
}