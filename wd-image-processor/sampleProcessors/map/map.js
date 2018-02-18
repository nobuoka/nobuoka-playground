var args = JSON.parse(arguments[0]) || [];
return new Promise(function (resolve, reject) {
  for (var i = 0; i + 1 < args.length; i += 2) {
    var x = args[i];
    var y = args[i + 1]
    var e = document.createElement("span");
    e.style.width = "2px";
    e.style.height = "2px";
    e.style.backgroundColor = "red";
    e.style.position = "absolute";
    e.style.top = (parseInt(x) - 1) + "px";
    e.style.left = (parseInt(y) - 1) + "px";
    document.body.appendChild(e);
  }
  var map = new ol.Map({
    target: 'map',
    layers: [
      new ol.layer.Tile({
        source: new ol.source.OSM()
      })
    ],
    view: new ol.View({
      center: ol.proj.fromLonLat([37.41, 8.82]),
      zoom: 4
    })
  });
  setTimeout(function () {
    resolve(args);
  }, 2000);
});
