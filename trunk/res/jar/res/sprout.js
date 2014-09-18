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
	_query('POST', '/login', 'ajax=true&mail=' + document.getElementById('mail').value + '&pass=' + document.getElementById('pass').value + '&remember=' + document.getElementById('remember').checked);
}
function ajaxPost(id) {
	_query('post', '/comment', 'ajax=true&body=' + encodeURIComponent(document.getElementById('body').value) + '&id=' + id);
}
function ajaxNick() {
	_query('post', '/nick', 'ajax=true&name=' + document.getElementById('name').value);
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
var request;
function _query(method, url, data) {
  request = create();
  request.open(method, url, true);
  request.onreadystatechange = sproutCallback;
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
  if(data.name == 'found') {
    var name = document.getElementById('name');
    name.style.borderColor = '#f60';
    name.style.borderStyle = 'solid';
  }
  if(data.name == 'available') {
    var name = document.getElementById('name');
    name.style.borderColor = '#6c3';
    name.style.borderStyle = 'solid';
  }
}
function sproutCallback() {
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