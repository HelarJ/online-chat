var socket = new WebSocket("ws://127.0.0.1:1337");

socket.onopen = function(event) {
    console.log("[open] Connection established");
    appendMessage("Connection established")
  };
  
  socket.onmessage = function(event) {
    console.log(`[message] Data received from server: ${event.data}`);
    appendMessage(event.data);
    let messagediv = document.getElementById("messagediv");
    messagediv.scrollBy(0, window.innerHeight);


  };
  
  socket.onclose = function(event) {
    console.log(`[close] Connection closed, code=${event.code} reason=${event.reason}`);
    appendMessage("Connection closed")
    let messagediv = document.getElementById("messagediv");
    messagediv.scrollBy(0, window.innerHeight);
  };
  
  socket.onerror = function(error) {
    console.log(`[error] ${error.message}`);
  };

  function appendMessage(msg){
    console.log(msg);
    let table = document.getElementById("table");
    let newtablerow = document.createElement("tr")
    try {
      let messageJSON = JSON.parse(msg);
      
      let timestamp = document.createElement("td");
      timestamp.innerText=messageJSON["timestamp"]
      newtablerow.appendChild(timestamp);

      let channelname = document.createElement("td");
      channelname.innerText="#"+messageJSON["channelName"]
      newtablerow.appendChild(channelname);

      let username = document.createElement("td");
      username.innerText=messageJSON["username"]+":"
      newtablerow.appendChild(username);

      
      
      let message = document.createElement("td");
      message.innerText=messageJSON["message"]
      newtablerow.appendChild(message);

    } catch(err){ //error tuleb JSON parsemisest, kui client saadab oma notificationeid, mis ei ole json formaadis.

      let message = document.createElement("td");
      message.innerText=msg
      newtablerow.appendChild(message);

    }
    

    
    
    

    table.appendChild(newtablerow)
  }

  function sendMessage(message){
      socket.send(message);
      document.getElementById('msg').value = "";
  }

  var input = document.getElementById("msg");

  input.addEventListener("keyup", function(event) {
    if (event.keyCode === 13) {
      event.preventDefault();
      document.getElementById("submitbtn").click();
    }
  });

