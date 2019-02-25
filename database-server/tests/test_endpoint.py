import pytest
import requests
class TestEndpoint(object):
    endpoint = "http://endpoint"
    def test_connection(self):
        r = requests.get(url=self.endpoint)
        assert r.json()!=None