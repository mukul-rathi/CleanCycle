const SCALE = 524288;
const OFFSET = 180;

// returns bit in src at the specified index
function getBit(src, index) {
    var byteIndex = Math.floor(index/8);
    var bitIndex = Math.floor(index%8);
    return src[byteIndex]>>(7-bitIndex) & 1;
}

// decode the input bytes and return the values as a 32 element array of numbers
function Decoder(bytes, port) {
    // set up the return array
    var decoded = [0, 0, 0, 0, 0, 0, 0, 0,
                   0, 0, 0, 0, 0, 0, 0, 0,
                   0, 0, 0, 0, 0, 0, 0, 0,
                   0, 0, 0, 0, 0, 0, 0, 0];

    //read in base latitude and longitude
    var baseLatitude = (bytes[0]<<9) + (bytes[1]<<1) + (bytes[2]>>>7);
    baseLatitude <<= 11;

    var baseLongitude = ((bytes[2] % 128)<<10) + (bytes[3]<<2) + (bytes[4]>>>6);
    baseLongitude <<= 11;

    // read in the lower bits of each datapoint
    bitIndex = 34;
    for (var i = 0; i < 8; i++) {
        decoded[i*4] = baseLatitude;
        for (var j = 0; j < 11; j++) {
            decoded[i*4] += getBit(bytes, bitIndex)<<(10-j);
            bitIndex++;
        }

        decoded[i*4 + 1] = baseLongitude;
        for (var j = 0; j < 11; j++) {
            decoded[i*4+1] += getBit(bytes, bitIndex)<<(10-j);
            bitIndex++;
        }

        var temp = 0
        for (var j = 0; j < 10; j++) {
            temp += getBit(bytes, bitIndex)<<(9-j);
            bitIndex++;
        }
        decoded[i*4 + 2] = temp;

        temp = 0;
        for (var j = 0; j < 10; j++) {
            temp += getBit(bytes, bitIndex)<<(9-j);
            bitIndex++;
        }
        decoded[i*4 + 3] = temp;
    }

    // normalize the latitude and longitude
    for (var i = 0; i < 8; i++) {
        decoded[4*i] = decoded[4*i]/SCALE - OFFSET;
        decoded[4*i + 1] = decoded[4*i + 1]/SCALE - OFFSET;
    }

    return decoded;
}
