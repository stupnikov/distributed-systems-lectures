FROM ubuntu:20.04
RUN apt-get update && apt-get install iputils-ping -y
ENTRYPOINT ["/bin/ping"]
CMD ["localhost"]