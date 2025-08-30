package handler

type tc struct {
	name       string
	method     string
	url        string
	headers    map[string]string
	token      string
	body       string
	wantStatus int
}
