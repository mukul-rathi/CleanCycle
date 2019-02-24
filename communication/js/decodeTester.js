load("decoder.js");

function stringToBytes(input) {
    var toReturn = [0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
                    0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
                    0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
                    0,0,0];

    var inputAsCharArray = input.split("");
    var counter = 0;
    for (var i = 0; i < 51; i++) {
        for (var j = 0; j < 8; j++) {
            toReturn[i] += parseInt(inputAsCharArray[8*i + j])<<(7-j);
        }
    }
    return toReturn;
}

var contents = readFile("../encoded.txt");
var originals = readFile("../inputs.txt");
var textArray = contents.split("\n");
var floatTextArray = originals.split("\n");
var floatArray = [];
var decoded = [];

for (var i = 0; i < floatTextArray.length; i++) {
    floatsSplit = floatTextArray[i].split(","); 
    floatArray.push([parseFloat(floatsSplit[0]),
                     parseFloat(floatsSplit[1]),
                     parseFloat(floatsSplit[2]),
                     parseFloat(floatsSplit[3])]);
}

for (var i = 0; i < textArray.length; i++) {
    decoded.push(Decoder(stringToBytes(textArray[i]), 0));
}

var correct = true;
for (var i = 0; i < 1024; i+=4) {
    var diff = floatArray[Math.floor(i/4)][i%4] - decoded[Math.floor(i/32)][i%32];
    if (diff > 0.00001) {
        correct = false;
        break;
    }
}

print(correct ? "Test succeeded" : "Test failed");
