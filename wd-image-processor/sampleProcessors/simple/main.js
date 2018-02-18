var args = JSON.parse(arguments[0]) || "Hello world!";
for (var i = 0; i < 40; i++) {
    var e = document.createElement("span");
    e.textContent = args;
    e.style.position = "absolute";
    e.style.top = (i * 11) + "px";
    e.style.left = (i * 14) + "px";
    document.body.appendChild(e);
}
