package main

import (
	"fmt"
	"io"
	"io/ioutil"
	"log"
	"math/rand"
	"net/http"
	"strconv"
)

func AdMobAdsServer(w http.ResponseWriter, req *http.Request) {
	i := rand.Intn(4) + 1
	data, err := ioutil.ReadFile("admob_mock-resp" + strconv.Itoa(i) + ".json")
	if err != nil {
		fmt.Println("File reading error", err)
		return
	}
	s := string(data)

	io.WriteString(w, s)
}
func AdMobConfigServer(w http.ResponseWriter, req *http.Request) {

	data, err := ioutil.ReadFile("admob_config.txt")
	if err != nil {
		fmt.Println("File reading error", err)
		return
	}
	s := string(data)

	io.WriteString(w, s)
}

func main() {
	rand.Seed(86)
	http.HandleFunc("/admob/ads", AdMobAdsServer)
	http.HandleFunc("/admob/config", AdMobConfigServer)

	fmt.Println("Server is going to start..")
	err := http.ListenAndServe(":9090", nil)
	fmt.Println("Server is going to quit..")
	if err != nil {
		log.Fatal("ListenAndServe: ", err)
	}
}
