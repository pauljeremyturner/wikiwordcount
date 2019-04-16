function map() {

emit(this.word, {counts: this.count});
}

/*

[
{"id" : "00001", "wordCounts" : [
    {"cat" : 999}, {"dog" : 998}, {"mouse" : 1}
    ]
},

{"id" : "00002", "wordCounts" : [{"apple" : 123}, {"pear" : 76}, {"banana" : 1}]},
{"id" : "00003", "wordCounts" : [{"the" : 555}, {"that" : 111}, {"spam" : 4}]}
]




*/