var socket = new WebSocket("ws://127.0.0.1:1337");

socket.onopen = function(event) {
    console.log("[open] Connection established");
    appendMessage("Connection established.")
  };
  
  socket.onmessage = function(event) {
    console.log(`[message] Data received from server: ${event.data}`);
    appendMessage(event.data);

  };
  
  socket.onclose = function(event) {
    console.log(`[close] Connection closed, code=${event.code} reason=${event.reason}`);
    appendMessage("Connection closed.")
  };
  
  socket.onerror = function(error) {
    console.log(`[error] ${error.message}`);
  };

  function appendMessage(message){
    table = document.getElementById("table");
    newtd = document.createElement("td")
    newtr = document.createElement("tr")
    newtd.innerHTML = message;
    newtr.appendChild(newtd)
    table.appendChild(newtr)
  }

  function sendMessage(message){
      socket.send(message);
  }

  var input = document.getElementById("msg");

  input.addEventListener("keyup", function(event) {
    if (event.keyCode === 13) {
      event.preventDefault();
      document.getElementById("submitbtn").click();
    }
  });

