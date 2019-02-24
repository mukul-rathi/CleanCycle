import pytest
import requests
class TestEndpoint(object):
    endpoint = "http://host.docker.internal:5000/"
    def test_connection(self):
        r = requests.get(url=self.endpoint)
        assert r.json()!=None